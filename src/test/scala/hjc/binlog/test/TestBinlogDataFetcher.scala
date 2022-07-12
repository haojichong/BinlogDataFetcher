package hjc.binlog.test

import hjc.binlog.client.BinlogClient
import hjc.binlog.common.{BinlogRow, MysqlCDCSource, StartupMode}
// TODO
object TestBinlogDataFetcher {
  def main(args: Array[String]): Unit = {
    val mysqlCDCSource: MysqlCDCSource = MysqlCDCSource(
      host = "127.0.0.1",
      port = 3306,
      user = "root",
      password = "root",
      schema = "test",
      table = "test1,test2,test3",
      startupMode = StartupMode.INITIAL,
      automation = false,
      prop = Map.empty
    )

    val binlogClient: BinlogClient = new BinlogClient(mysqlCDCSource, "test-1") {
      /**
       * 处理逻辑由用户来定义
       */
      override def handle(binlogRow: BinlogRow): Unit = {
        println(binlogRow)
      }
    }
    // 开始采集
    binlogClient.open()
    // 结束采集
    // binlogClient.close()
  }
}
