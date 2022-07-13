package hjc.binlog.test.notify

import hjc.binlog.common.{Contact, Mapper, SystemNotify, VoiceSource}

object VoiceTest extends Mapper with App {

  case class User(userName: String, email: String, phone: String)

  val user = User(
    userName = "hjc",
    email = "test@test",
    phone = "1234567"
  )
  /**
   * appId,appKey,templateId 需要自己申请
   */
  val conf: VoiceSource = VoiceSource(
    appId = 11111111,
    appKey = "qqqqqqqqqq",
    templateId = 2222222, // 通话模板
    numberOfBroadcasts = 2, // 通话内容播报次数
  )
  SystemNotify.apply(conf).get
    .setContact(List(Contact(user.userName, Some(user.email), Some(user.phone)))) // 设置联系人电话号码
    .send(s"测试电话通知123哒哒哒")
}
