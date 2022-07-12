package hjc.binlog.common

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility
import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.core.{JsonParseException, JsonParser, JsonProcessingException}
import com.fasterxml.jackson.databind.{DeserializationFeature, JsonMappingException, ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.module.scala.DefaultScalaModule

import java.text.SimpleDateFormat

/**
 * json字符串序列化与反序列化的接口，提供两个方法，一个将json字符串反序列化为对象，一个将对象序列化成json字符串
 */
trait Mapper {
  lazy val mapper: ObjectMapper = {
    val _mapper = new ObjectMapper()

    // 设置哪些需要需要序列化，目前只序列化 public 字段
    _mapper.setVisibility(PropertyAccessor.FIELD, Visibility.PUBLIC_ONLY)

    // 支持 scala 基本数据类型
    _mapper.registerModule(DefaultScalaModule)

    // 序列化存在且不为null的字段 `https://blog.csdn.net/Pa_Java/article/details/109482223`
    _mapper.setSerializationInclusion(Include.NON_ABSENT)

    // 是否将时间转换为时间戳格式保存，配合setDateFormat可以输出日期时间格式
    _mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    _mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"))

    // 序列化枚举类型为数字
    _mapper.configure(SerializationFeature.WRITE_ENUMS_USING_INDEX, true)

    // 反序列化的时候遇到不能识别的字段，是否失败（抛出异常）
    _mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    //    _mapper.getSerializerFactory.withSerializerModifier(new SageBeanSerializerModifier)
    _mapper
  }

  /**
   * 将json字符串反序列化成对象
   *
   * @tparam T 对象类型
   * @param json  待反序列化的json字符串
   * @param clazz 指定对象类型的Class
   * @return 反序列化后的对象
   * @throws JsonParseException   如果json字符串中包含{@link JsonParser}不支持的数据类型将抛出该异常
   * @throws JsonMappingException 如果输入 JSON 结构与结果类型预期的结构不匹配（或有其他不匹配问题）
   * @since 1.0
   * */
  def json2Obj[T](json: String, clazz: Class[T]): T = {
    mapper.readValue(json, clazz)
  }

  /**
   * 将json字符串反序列化成List
   *
   * @param json          待反序列化的json字符串
   * @param typeReference 指定List类型的TypeReference
   * @tparam T List中的元素类型
   * @return T 类型的元素列表
   */
  def json2List[T](json: String, typeReference: TypeReference[List[T]]): List[T] = {
    mapper.readValue(json, typeReference)
  }

  def json2Obj[T](json: String, typeReference: TypeReference[T]): T = {
    mapper.readValue(json, typeReference)
  }

  /**
   * 将对象序列化成json字符串
   *
   * @param obj    待序列化的对象
   * @param pretty 是否格式化
   * @return 序列化后的json字符串
   * @throws JsonProcessingException 序列化过程中可能抛出该异常
   * @since 1.0
   */
  def obj2Json(obj: Any, pretty: Boolean = false): String = {
    if (pretty) mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj) else mapper.writeValueAsString(obj)
  }
}
