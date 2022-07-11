package hjc.binlog.client

import com.github.shyiko.mysql.binlog.BinaryLogClient
import com.github.shyiko.mysql.binlog.event._
import hjc.binlog.client.BinlogClient._
import hjc.binlog.common.StartupMode.StartupOptions
import hjc.binlog.common.{Logger, MysqlCDCSource, StartupMode}
import hjc.binlog.store.DbBinlogOffsetStore
import hjc.binlog.table.DbBinlogOffsetRow
import hjc.binlog.tools.{BinlogRow, JDBCUtils}
import org.slf4j

//import java.sql.{Connection, DriverManager, ResultSet, ResultSetMetaData, SQLException}
import java.sql.{Connection, DriverManager, ResultSet, ResultSetMetaData, SQLException}
import java.text.SimpleDateFormat
import java.util.Date
import java.{io, util}
import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
 * Binlog客户端
 *
 * @param conf     数据源配置信息
 * @param clientId 客户端ID标识
 */
abstract class BinlogClient(val conf: MysqlCDCSource, val clientId: String = "") {
  // mysql数据库源基本信息
  private lazy val host: String = conf.host
  private lazy val port: Int = conf.port
  private lazy val schema: String = conf.schema
  private lazy val table: String = conf.table
  private lazy val user: String = conf.user
  private lazy val password: String = conf.password
  private lazy val startupMode: StartupOptions = conf.startupMode
  private lazy val prop: Map[String, Any] = conf.prop
  private lazy val logger: slf4j.Logger = Logger.apply("BinlogClient").logger
  private lazy val extendData: Map[String, Any] = prop.get("extendData") match {
    case Some(value) => value.asInstanceOf[Map[String, Any]]
    case None => Map[String, Any]()
  }
  private val tableAndSql: Map[String, String] = prop.get("sql") match {
    case Some(value) => value.asInstanceOf[Map[String, String]]
    case None => Map[String, String]()
  }
  private val tableList: List[String] = if (table.isEmpty) {
    JDBCUtils.getTableList(schema, getConnection.get)
  } else {
    table.split(",").toList
  }
  private lazy val tableNameList: String = tableList.mkString("[", ",", "]")
  // binlog相关属性
  private var binLogClient: Option[BinaryLogClient] = None
  private val columnNameCache: mutable.Map[String, List[String]] = mutable.Map[String, List[String]]()
  private var conn: Option[Connection] = None
  private var offset: (String, Long) = ("", 0)
  private var fullFinishedFlag = false

  /**
   * 初始化
   */
  def open(): Unit = {
    binLogClient = Some(new BinaryLogClient(host, port, user, password))
    if (getConnection.nonEmpty) {
      initMetaData()
      binLogClient.get.setServerId((host + schema + table).hashCode.toLong)
      startupMode match {
        case StartupMode.FULL_ONLY =>
          fullSync()
        case StartupMode.INITIAL =>
          if (getBinlogOffset(clientId, host, schema, tableNameList).isEmpty) {
            fullSync()
          } else {
            if (!conf.automation) fullSync()
          }
          setBinLogOffset(binLogClient.get, host, tableNameList)
        case StartupMode.READ_LAST =>
          try {
            setBinLogOffset(binLogClient.get, host, tableNameList)
          } catch {
            case _: Throwable =>
              logger.warn(s"Binlog断点续传采集任务[${clientId}], 未获取到偏移量信息,将从最新位置读取")
          }
        case _ => // 读取新
      }
      if (startupMode != StartupMode.FULL_ONLY) {
        binLogClient.get.registerEventListener(listenerEvents)
        binLogClient.get.connect()
      } else {
        close()
      }
    }
  }

  /**
   * 初始化元数据
   *
   * 1.所有表的字段名列表
   */
  def initMetaData(): Unit = {
    try {
      tableList.foreach((tableName: String) => {
        columnNameCache += (tableName -> JDBCUtils.getColumnNames(tableName, conn.get).map((_: (String, String))._1))
      })
    } catch {
      case exception: Exception =>
        logger.error(s"初始化异常,${exception.getMessage}")
        close()
    }
  }

  /**
   * 处理事件
   */
  private def handleEvents(binlogRowOption: Option[BinlogRow]): Unit = {
    offset = ("", 0)
    clear()
    if (binlogRowOption.nonEmpty) {
      handle(binlogRowOption.get)
      save(clientId, host, schema, tableNameList, binLogClient.get.getBinlogFilename, binLogClient.get.getBinlogPosition)
    }
  }

  /**
   * 全量同步是否结束
   */
  def isFullSyncFinished: Boolean = {
    fullFinishedFlag
  }

  /**
   * 处理逻辑由用户来定义
   */
  def handle(binlogRow: BinlogRow): Unit

  /**
   * 全量同步
   */
  def fullSync(): Unit = {
    if (savepoint()) {
      tableList.foreach((tableName: String) => {
        val sql: String = if (tableAndSql.contains(tableName)) {
          tableAndSql(tableName)
        } else {
          s"SELECT * FROM $schema.$tableName"
        }
        val rs: ResultSet = JDBCUtils.streamingQuery(conn.get, 1000, sql)
        val metaData: ResultSetMetaData = rs.getMetaData
        while (rs.next()) {
          val row: mutable.Map[String, Any] = mutable.Map[String, Any]()
          for (i <- 1 to metaData.getColumnCount) {
            row += ((metaData.getColumnLabel(i), rs.getObject(i)))
          }
          handle(BinlogRow(schema, tableName, "full", List(row.toMap ++ extendData)))
        }
        JDBCUtils.close(null, null, rs)
      })
      fullFinishedFlag = true
    } else {
      throw new Exception(s"Binlog偏移量保存异常,CDC同步失败")
    }
  }

  /**
   * 全量同步开始之前记录一个Binlog的低位Offset
   */
  private def savepoint(): Boolean = {
    lazy val client = new BinaryLogClient(host, conf.port, user, password)
    connect(client)
    val binlogFilename: String = client.getBinlogFilename
    val binlogPosition: Long = client.getBinlogPosition
    // 记录当前binlog文件名和当前位置
    try {
      save(clientId, host, schema, tableNameList, binlogFilename, binlogPosition)
    } catch {
      case exception: Exception =>
        logger.error(exception.getMessage)
        false
    }
  }

  /**
   * 设置Binlog偏移量
   */
  private def setBinLogOffset(client: BinaryLogClient, host: String, table: String) {
    getBinlogOffset(clientId, host, schema, table) match {
      case Some(value) =>
        client.setBinlogFilename(value.filename)
        client.setBinlogPosition(value.position)
        offset = (value.filename, value.position)
      case None =>
        throw new Exception(s"未获取到Binlog偏移量,CDC同步失败")
    }
  }

  /**
   * 监听事件
   */
  private def listenerEvents(event: Event): Unit = {
    event.getData[EventData] match {
      // 记录偏移量
      case data: RotateEventData => record((data.getBinlogFilename, data.getBinlogPosition))
      // 缓存表的元数据信息
      case data: TableMapEventData => put(data, schema, tableList)
      // Insert事件
      case data: WriteRowsEventData => handleEvents(InsertEvent(data))
      // Update事件
      case data: UpdateRowsEventData => handleEvents(UpdateEvent(data))
      // Delete事件
      case data: DeleteRowsEventData => handleEvents(DeleteEvent(data))
      // 其他事件不做处理
      case _ =>
    }
  }


  /**
   * 获取表的字段名列表
   */
  def getColumnNameList(tableName: String): List[String] = {
    columnNameCache.get(tableName) match {
      case Some(value) => value
      case None =>
        val columnList: List[String] = JDBCUtils.getColumnNames(tableName, conn.get).map((_: (String, String))._1)
        columnNameCache += (tableName -> columnList)
        columnList
    }
  }

  /**
   * Update事件
   */
  private def UpdateEvent(eventData: UpdateRowsEventData): Option[BinlogRow] = {
    val tableMetadata: TableMapEventData = getMetadata(eventData.getTableId)
    if (check && tableMetadata != null && tableMetadata.getDatabase == schema && tableList.contains(tableMetadata.getTable)) {
      val tableName: String = tableMetadata.getTable
      val column: List[String] = getColumnNameList(tableName)
      val originDataAndNewData: List[(List[String], List[String])] = eventData.getRows.asScala.toList.map((rows: util.Map.Entry[Array[io.Serializable], Array[io.Serializable]]) => {
        rows.getKey.toList.map(format) -> rows.getValue.toList.map(format)
      })
      val data: List[Map[String, String]] = originDataAndNewData.map(data => column.zip(data._2).toMap)
      val beforeData: List[Map[String, String]] = originDataAndNewData.map(data => column.zip(data._1).toMap)
      Some(BinlogRow(schema, tableName, "update", data.map(_ ++ extendData), beforeData.map(_ ++ extendData)))
    } else {
      None
    }
  }

  /**
   * Insert事件
   */
  private def InsertEvent(eventData: WriteRowsEventData): Option[BinlogRow] = {
    val tableMetadata: TableMapEventData = getMetadata(eventData.getTableId)
    if (check && tableMetadata != null && tableMetadata.getDatabase == schema && tableList.contains(tableMetadata.getTable)) {
      val tableName: String = tableMetadata.getTable
      val column: List[String] = getColumnNameList(tableName)
      val data: List[Map[String, String]] = eventData.getRows.asScala.toList.map((value: scala.Array[io.Serializable]) => {
        column.zip(value.map(format)).toMap
      })
      Some(BinlogRow(schema, tableName, "create", data.map(_ ++ extendData)))
    } else {
      None
    }
  }

  /**
   * Delete 事件
   */
  private def DeleteEvent(eventData: DeleteRowsEventData): Option[BinlogRow] = {
    val tableMetadata: TableMapEventData = getMetadata(eventData.getTableId)
    if (check && tableMetadata != null && tableMetadata.getDatabase == schema && tableList.contains(tableMetadata.getTable)) {
      val tableName: String = tableMetadata.getTable
      val column: List[String] = getColumnNameList(tableName)
      val data: List[Map[String, String]] = eventData.getRows.asScala.toList.map((value: scala.Array[io.Serializable]) => {
        column.zip(value.map(format)).toMap
      })
      Some(BinlogRow(schema, tableName, "delete", data.map(_ ++ extendData)))
    } else {
      None
    }
  }

  /**
   * 校验,主要是校验启动模式为 read_last模式,因为此模式启动时发出的第一条数据总是重复的,在此做了检验以及过滤数据
   */
  def check: Boolean = {
    if (startupMode == StartupMode.READ_LAST) {
      !getOffset.contains(offset)
    } else {
      true
    }
  }

  /**
   * 获取数据库连接
   */
  private def getConnection: Option[Connection] = {
    try {
      if (conn.isEmpty)
        conn = Some(DriverManager.getConnection(conf.url, user, password))
    } catch {
      case e: SQLException =>
        logger.error(s"任务启动失败,[${conf.url}] get connection failure:" + e.getMessage)
    }
    conn
  }

  /**
   * 关闭
   */
  def close(): Unit = {
    if (conn.nonEmpty)
      JDBCUtils.closeConnection(conn.get)
    if (binLogClient.nonEmpty)
      binLogClient.get.disconnect()
    fullFinishedFlag = true
    logger.info(s"BinLog采集任务[${clientId}]已结束")
  }

  /**
   * 临时连接binlog客户端,主要用于在全量采集数据前,获取偏移量低位点信息
   */
  private def connect(client: BinaryLogClient): Unit = {
    val thread: Thread = new Thread() {
      override def run(): Unit = {
        client.setServerId(System.currentTimeMillis() / 10000)
        client.connect()
      }
    }
    thread.start()
    Thread.sleep(1000L)
    thread.interrupt()
    client.disconnect()
  }

  /**
   * 格式化数据
   *
   * io.Serializable => String
   */
  private def format(row: Any): String = {
    if (row != null) {
      if (List(classOf[Byte], classOf[Short], classOf[Int], classOf[Long], classOf[Float], classOf[Double], classOf[Char], classOf[String]).contains(row.getClass)) {
        String.valueOf(row)
      } else if (row.isInstanceOf[Date]) {
        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(row)
      } else row match {
        case bool: Boolean =>
          if (bool) "1" else "0"
        case _ =>
          row match {
            case array: scala.Array[Byte] => new String(array)
            case _ => row.toString
          }
      }
    } else {
      ""
    }
  }
}

/**
 * 用于存储表的元数据信息和偏移量
 */
object BinlogClient {
  private[this] lazy val metadataCache: ListBuffer[TableMapEventData] = ListBuffer[TableMapEventData]()
  private[this] lazy val offsetCache: ListBuffer[(String, Long)] = ListBuffer()
  private[this] lazy val offsetStore: DbBinlogOffsetStore = new DbBinlogOffsetStore

  /**
   * 添加缓存
   */
  private def put(data: TableMapEventData, schema: String, tableList: List[String]): Unit = {
    if (data.getDatabase == schema && tableList.contains(data.getTable))
      this.metadataCache += data
  }

  /**
   * 记录偏移量
   */
  private def record(offset: (String, Long)): Unit = {
    this.offsetCache += offset
  }

  /**
   * 获取偏移量
   */
  private def getOffset: List[(String, Long)] = {
    this.offsetCache.toList
  }

  /**
   * 清空缓存
   */
  private def clear(): Unit = {
    this.offsetCache.clear()
  }

  /**
   * 获取元数据
   */
  private def getMetadata(tableId: Long): TableMapEventData = {
    try {
      this.metadataCache.filter((_: TableMapEventData).getTableId == tableId).head
    } catch {
      case _: Throwable =>
        null
    }
  }

  /**
   * 保存binlog文件名和当前读取的位置
   */
  private def save(clientId: String, ip: String, schema: String, table: String, binLogFileName: String, position: Long): Boolean = {
    offsetStore.save(clientId, ip, schema, table, binLogFileName, position)
  }

  /**
   * 获取Binlog偏移量
   */
  private def getBinlogOffset(clientId: String, ip: String, schema: String, table: String): Option[DbBinlogOffsetRow] = {
    offsetStore.getByIpSchemaTable(clientId, ip, schema, table)
  }
}