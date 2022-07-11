package hjc.binlog.common

/**
 * mysql source
 * @param host IP
 * @param port 端口
 * @param user 用户
 * @param password 密码
 * @param schema 采集的数据库
 * @param table 采集的表,多张表逗号隔开(暂不支持 库.表名 写法)
 * @param startupMode 默认启动模式为 Initial
 * @param automation  仅在启动模式为`INITIAL`使用;为True时,仅初次运行任务时才会跑全量,其他情况是读取上次,默认False
 * @param prop 额外配置(可以用来补充数据,或是指定SQL全量同步)
 */
final case class MysqlCDCSource(host: String,
                                port: Int,
                                user: String,
                                password: String,
                                schema: String,
                                table: String = "",
                                startupMode: StartupMode.StartupOptions = StartupMode.INITIAL,
                                automation: Boolean = false,
                                prop: Map[String, Any] = Map.empty) {
  lazy val url = s"jdbc:mysql://$host:$port/$schema?tinyInt1isBit=false&useSSL=false&transformedBitIsBoolean=false&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=Asia/Shanghai"
}

/**
 * MySQL-CDC 启动模式选项
 */
object StartupMode extends Enumeration {
  type StartupOptions = String
  // 全量同步完成后再增量同步
  final val INITIAL = "initial"
  // 只进行全量采集
  final val FULL_ONLY = "fullOnly"
  // 从最新的偏移量开始读
  final val LATEST = "latest"
  // 从上次保存的偏移量读取[断点续传]
  final val READ_LAST = "readLast"
}