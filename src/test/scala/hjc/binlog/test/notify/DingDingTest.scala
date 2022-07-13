package hjc.binlog.test.notify

import hjc.binlog.common._

object DingDingTest extends Mapper with App {

  case class User(userName: String, email: String, phone: String)

  val user = User(
    userName = "hjc",
    email = "test@test",
    phone = "1234567"
  )

  val conf: DingDingSource = DingDingSource(
    url = "https://oapi.dingtalk.com/robot/send?access_token=4bb0d4a094ecf035059bf776a98ca50d8521efc17311bf7d0a4311359956cfde",
    notifyType = DingDingNotifyType.MARKDOWN,
    isAtAll = false,
  )
  SystemNotify.apply(conf).get
    .setContact(List(Contact(user.userName, Some(user.email), Some(user.phone)))) // 设置 @群成员列表
    .send("钉钉测试内容...", "标题")
}
