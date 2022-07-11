package hjc.binlog.store

import hjc.binlog.table.DbBinlogOffsetRow

// TODO 待办
/**
 * MySQL Binlog日志偏移量存储
 */
class DbBinlogOffsetStore {

  /**
   * 保存偏移量
   *
   * @param clientId       利用此ID区分采集任务
   * @param ip             数据库IP
   * @param schema         数据库名称
   * @param table          采集的表(可能多张)
   * @param binLogFileName binlog文件名
   * @param position       binlog位置
   * @return
   */
  def save(clientId: String, ip: String, schema: String, table: String, binLogFileName: String, position: Long): Boolean = {
    // TODO
    true
  }

  /**
   * 获取偏移量信息
   */
  def getByIpSchemaTable(clientId: String, ip: String, schema: String, table: String): Option[DbBinlogOffsetRow] = {
    // TODO
    None
  }
}
