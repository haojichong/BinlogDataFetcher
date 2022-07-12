package hjc.binlog.common

/**
 * BinlogRow
 *
 * @param schema     数据库
 * @param table      表
 * @param operate    操作类型
 * @param data       数据
 * @param beforeData 原始数据(更新之前的旧数据,插入和删除此列为空)
 */
final case class BinlogRow(
                            schema: String,
                            table: String,
                            operate: String,
                            data: List[Map[String, Any]],
                            beforeData: List[Map[String, Any]] = List.empty
                          )
