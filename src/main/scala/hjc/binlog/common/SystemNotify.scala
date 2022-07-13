package hjc.binlog.common


import hjc.binlog.tools.ClassUtils

import scala.util.Try

/**
 * 系统通知接口
 */
trait SystemNotify {

  def open(): SystemNotify

  def conf: NotifySource

  /**
   * 设置联系人
   *
   * @param contact 联系人[电话联系人|邮件收件/抄送人|钉钉被@人]
   */
  def setContact(contact: List[Contact]): SystemNotify

  /**
   * 发送通知
   *
   * @param content 发送内容
   * @param title   标题[邮件标题|钉钉标题]
   */
  def send(content: String, title: String = ""): Unit
}

/**
 * 系统通知类构建
 */
object SystemNotify {

  private lazy val classes: Map[Class[_], Class[_ <: SystemNotify]] = ClassUtils.subClass(classOf[SystemNotify], name = "hjc.binlog.alert")

  def apply(conf: NotifySource): Option[SystemNotify] = Try {
    val key: Class[_ <: NotifySource] = conf.getClass
    classes(key).getConstructor(key).newInstance(conf).open()
  }.toOption
}