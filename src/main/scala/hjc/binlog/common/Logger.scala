package hjc.binlog.common


import org.slf4j.LoggerFactory

/**
 * Logger 记录日志
 */
trait Logger {

  protected def loggerName: String = this.getClass.getCanonicalName

  lazy val logger: org.slf4j.Logger = LoggerFactory.getLogger(loggerName)
}

/**
 * 方便自定义logger，一种通过Class来创建，一种通过loggerName来创建
 */
object Logger {
  /**
   * 根据指定类创建Logger实例
   */
  def apply[T](clazz: Class[T]): Logger = new Logger {
    override val loggerName: String = clazz.getCanonicalName
  }

  /**
   * 根据给定的名字生成Logger实例
   */
  def apply(name: String): Logger = new Logger {
    override val loggerName: String = name
  }
}