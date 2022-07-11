package hjc.binlog.tools

import hjc.binlog.common.Logger

import java.sql._
import scala.collection.mutable.ListBuffer

/**
 * JDBC 工具类
 */
object JDBCUtils extends Logger {
  private val SQL = "SELECT * FROM "

  /**
   * 获取数据库连接
   */
  def getConnection(url: String, username: String, password: String): Connection = {
    try {
      DriverManager.getConnection(url, username, password)
    } catch {
      case e: SQLException =>
        logger.error(s"[$url] get connection failure:" + e.getMessage)
        null
    }
  }

  /**
   * 关闭数据库连接
   */
  def closeConnection(conn: Connection) {
    if (conn != null) {
      try {
        conn.close();
      } catch {
        case e: SQLException =>
          logger.error("close connection failure:" + e.getMessage)
      }
    }
  }

  /**
   * 获取指定库下所有的表名
   *
   * @param schema 数据库名称
   * @param conn   数据库连接
   * @return 表名列表
   */
  def getTableList(schema: String, conn: Connection): List[String] = {
    val tableList: ListBuffer[String] = ListBuffer()
    val metaData: DatabaseMetaData = conn.getMetaData
    val types: scala.Array[String] = scala.Array("TABLE")
    val table: ResultSet = metaData.getTables(schema, null, "%", types)
    while (table.next()) {
      tableList += (table.getString("TABLE_NAME"))
    }
    tableList.toList
  }

  /**
   * 获取表中所有字段名称和类型
   *
   * @param tableName 表名
   * @param conn      连接
   * @return (字段名,字段类型)的一个List
   */
  def getColumnNames(tableName: String, conn: Connection): List[(String, String)] = {
    val columns = new ListBuffer[(String, String)]()
    var pStmt: PreparedStatement = null
    val tableSql: String = SQL + tableName
    try {
      pStmt = conn.prepareStatement(tableSql)
      val metadata: ResultSetMetaData = pStmt.getMetaData
      val size: Int = metadata.getColumnCount
      for (i <- 1 to size) {
        columns += (metadata.getColumnName(i) -> metadata.getColumnTypeName(i))
      }
    } catch {
      case e: SQLException =>
        logger.error("getColumnNames failure:" + e.getMessage)
    } finally {
      if (pStmt != null) {
        try {
          pStmt.close()
        } catch {
          case e: SQLException =>
            logger.error("getColumnNames close pstem and connection failure:" + e.getMessage)
        }
      }
    }
    columns.toList
  }

  /**
   * 流式查询
   */
  def streamingQuery(conn: Connection, fetchSize: Int, sql: String): ResultSet = {
    query(conn, "stream", sql, fetchSize)
  }

  /**
   * 全量查询
   */
  def fullQuery(conn: Connection, sql: String): ResultSet = {
    query(conn, "full", sql)
  }

  /**
   * 单表查询
   *
   * @param conn   连接
   * @param `type` 类型(流式或全量)
   * @param sql    SQL语句
   * @return 查询的结果集
   */
  def query(conn: Connection, `type`: String, sql: String, fetchSize: Int = Integer.MIN_VALUE): ResultSet = {
    var statement: PreparedStatement = null
    var resultSet: ResultSet = null
    `type` match {
      case "stream" => {
        statement = conn.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)
        statement.setFetchSize(fetchSize)
        resultSet = statement.executeQuery()
        resultSet
      }
      case "full" => {
        statement = conn.prepareStatement(sql)
        resultSet = statement.executeQuery()
        resultSet
      }
    }
  }

  /**
   * 关闭数据库资源
   */
  def close(conn: Connection, stmt: PreparedStatement, rs: ResultSet): Unit = {
    if (rs != null) {
      try {
        rs.close()
      } catch {
        case e: SQLException =>
          e.printStackTrace()
      }
    }
    if (stmt != null) {
      try {
        stmt.close()
      } catch {
        case e: SQLException =>
          e.printStackTrace()
      }
    }
    if (conn != null) {
      try {
        conn.close()
      } catch {
        case e: SQLException =>
          e.printStackTrace()
      }
    }
  }
}