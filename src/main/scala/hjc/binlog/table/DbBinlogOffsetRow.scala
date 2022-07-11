package hjc.binlog.table

/**
 * Binlog偏移量Bean类
 *
 * @param id       自增ID
 * @param clientId 客户端ID/任务ID 用来区分相同配置的不同任务
 * @param ip       数据库IP
 * @param schema   数据库
 * @param table    数据表
 * @param filename binlog文件名
 * @param position binlog当前位置
 */
case class DbBinlogOffsetRow(
                              id: Long,
                              clientId: String,
                              ip: String,
                              schema: String,
                              table: String,
                              filename: String,
                              position: Long
                            )
