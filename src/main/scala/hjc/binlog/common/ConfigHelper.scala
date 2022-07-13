package hjc.binlog.common

import com.typesafe.config.{Config, ConfigFactory}

/**
 * 获取配置文件属性
 */
object ConfigHelper {
  lazy val binlogConfig: ConfigHelper = ConfigHelper()

  private def apply(): ConfigHelper = {
    new ConfigHelper()
  }

  def load(name: String): Config = ConfigFactory.load(name)
}

class ConfigHelper() extends Mapper {

  import scala.collection.JavaConverters._

  private var binlog: Option[Config] = None

  def config: Config = binlog.get

  def dbConfig: Config = config.getConfig("binlog.db")

  def intValue(key: String, default: Int = 0): Int = {
    try {
      config.getInt(key)
    } catch {
      case _: Throwable =>
        default
    }
  }

  def longValue(key: String, default: Long = 0L): Long = {
    try {
      config.getLong(key)
    } catch {
      case _: Throwable =>
        default
    }
  }

  def stringValue(key: String, default: String = ""): String = {
    try {
      config.getString(key)
    } catch {
      case _: Throwable =>
        default
    }
  }

  def booleanValue(key: String, default: Boolean = false): Boolean = {
    try {
      config.getBoolean(key)
    } catch {
      case _: Throwable =>
        default
    }
  }

  def listValue[T](key: String): List[T] = config.getList(key).unwrapped().asScala.map(_.asInstanceOf[T]).toList

  def listValue[T](key: String, default: List[T]): List[T] = {
    val list = listValue[T](key)
    if (list.nonEmpty) list else default
  }
}