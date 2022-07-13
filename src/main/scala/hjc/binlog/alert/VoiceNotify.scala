package hjc.binlog.alert

import com.github.qcloudsms.httpclient.HTTPException
import com.github.qcloudsms.{TtsVoiceSender, TtsVoiceSenderResult}
import hjc.binlog.common.{Contact, Logger, SystemNotify, VoiceSource}
import org.json.JSONException

import java.io.IOException
import scala.util.Try

/**
 * 语音播报通知
 */
class VoiceNotify(override val conf: VoiceSource) extends Logger with SystemNotify {
  private var voiceSender: Option[TtsVoiceSender] = None
  private var phoneList: List[String] = List.empty

  override def open(): SystemNotify = {
    voiceSender = Try(Some(new TtsVoiceSender(conf.appId, conf.appKey))).getOrElse(None)
    this
  }

  override def setContact(contact: List[Contact]): SystemNotify = {
    if (voiceSender.nonEmpty && contact.nonEmpty) {
      phoneList = (phoneList ++ contact.filter(data => data.phone.nonEmpty).map(_.phone.get)).distinct
    }
    this
  }

  override def send(content: String, title: String = ""): Unit = {
    if (voiceSender.nonEmpty) {
      try {
        phoneList.foreach((phone: String) => {
          val rt: TtsVoiceSenderResult = voiceSender.get.send(
            "86",
            phone,
            conf.templateId,
            Array(content),
            conf.numberOfBroadcasts,
            ""
          )
          logger.info(s"Call for ${phone} with message[$content], receive http code = ${rt.getResponse.statusCode}, result = ${rt.result}, errMsg = ${rt.errMsg}, callId = ${rt.callid}")
        })
      } catch {
        case e: HTTPException => e.printStackTrace()
        case e: JSONException => e.printStackTrace()
        case e: IOException => e.printStackTrace()
        case e: Exception => e.printStackTrace()
      }
    }
  }
}