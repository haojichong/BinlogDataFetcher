package hjc.binlog.common

import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo.{As, Id}
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}

@JsonTypeInfo(use = Id.NAME, include = As.EXISTING_PROPERTY, property = "name")
@JsonSubTypes(Array(
  new Type(value = classOf[VoiceSource], name = "voice"),
  new Type(value = classOf[DingDingSource], name = "dingding"),
))
sealed trait NotifySource {
  val name: String
}
/**
 * 语音配置源
 *
 * @param appId              sdk appId
 * @param appKey             sdk appKey
 * @param templateId         模板ID
 * @param numberOfBroadcasts 通话内容播报次数
 */
final case class VoiceSource(
                              appId: Int = 0,
                              appKey: String = "",
                              templateId: Int,
                              numberOfBroadcasts: Int = 3
                            ) extends NotifySource {
  override val name: String = "voice"
}

/**
 * 钉钉通知配置源
 *
 * @param url        钉钉群机器人的Webhook地址
 * @param notifyType 钉钉通知类型[markdown,text]
 * @param isAtAll    是否 @所有人
 */
final case class DingDingSource(
                                 url: String,
                                 notifyType: DingDingNotifyType.NotifyType = DingDingNotifyType.MARKDOWN,
                                 isAtAll: Boolean = false,
                               ) extends NotifySource {
  override val name: String = "dingding"
}

/**
 * 联系方式
 *
 * @param name  姓名
 * @param email 邮箱
 * @param phone 电话
 */
case class Contact(
                    name: String,
                    email: Option[String] = None,
                    phone: Option[String] = None,
                  )

/**
 * 钉钉通知类型[Markdown|Text]
 */
object DingDingNotifyType extends Enumeration {
  type NotifyType = String
  final val MARKDOWN = "markdown"
  final val TEXT = "text"
}