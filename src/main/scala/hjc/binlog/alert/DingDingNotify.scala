package hjc.binlog.alert

import com.dingtalk.api.request.OapiRobotSendRequest
import com.dingtalk.api.request.OapiRobotSendRequest.At
import com.dingtalk.api.{DefaultDingTalkClient, DingTalkClient}
import hjc.binlog.common.{Contact, DingDingNotifyType, DingDingSource, SystemNotify}

import scala.collection.JavaConverters._
import scala.util.Try

/**
 * 钉钉机器人通知
 */
class DingDingNotify(override val conf: DingDingSource) extends SystemNotify {
  private var client: Option[DingTalkClient] = None
  private var phoneList: List[String] = List.empty
  private lazy val isAtAll: Boolean = conf.isAtAll

  override def open(): SystemNotify = {
    client = Try(Some(new DefaultDingTalkClient(conf.url))).getOrElse(None)
    this
  }

  override def setContact(contact: List[Contact]): SystemNotify = {
    if (client.nonEmpty && contact.nonEmpty) {
      phoneList = phoneList ++ contact.map(_.phone.getOrElse(""))
    }
    this
  }

  override def send(content: String, title: String): Unit = {
    if (client.nonEmpty) {
      val request: OapiRobotSendRequest = new OapiRobotSendRequest()
      val at = new At()
      conf.notifyType match {
        case DingDingNotifyType.MARKDOWN =>
          request.setMsgtype(DingDingNotifyType.MARKDOWN)
          val markdown = new OapiRobotSendRequest.Markdown
          markdown.setTitle(title)
          if (isAtAll) {
            at.setIsAtAll("true")
            markdown.setText(content)
          } else {
            at.setIsAtAll("false")
            if (phoneList.nonEmpty) {
              at.setAtMobiles(phoneList.asJava)
              markdown.setText(content + "\n>" + phoneList.map((m: String) => s"@$m").filter(!content.contains(_: String)).mkString(" "))
            } else {
              markdown.setText(content)
            }
          }
          request.setMarkdown(markdown)
          request.setAt(at)
        case DingDingNotifyType.TEXT =>
          request.setMsgtype(DingDingNotifyType.TEXT)
          val text: OapiRobotSendRequest.Text = new OapiRobotSendRequest.Text()
          text.setContent(content)
          request.setText(text)
          if (isAtAll) {
            at.setIsAtAll("true")
          } else {
            at.setIsAtAll("false")
            if (phoneList.nonEmpty)
              at.setAtMobiles(phoneList.asJava)
          }
      }
      request.setAt(at)
      client.get.execute(request)
    }
  }
}