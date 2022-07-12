package hjc.binlog.tools

import hjc.binlog.common.Mapper
import org.reflections.Reflections

import scala.collection.JavaConverters.asScalaSetConverter
import scala.util.Try
import scala.util.matching.Regex

object ClassUtils extends Mapper {

  class ClassUtils

  final def find[T](underlying: Class[T], name: String = "hjc.binlog"): Option[Class[_ <: T]] = {
    val reflects = new Reflections(name)
    reflects.getSubTypesOf(underlying).asScala.headOption
  }

  final def subClass[T](underlying: Class[T], params: Int = 1, name: String = "hjc.binlog", filter: Option[String] = None): Map[Class[_], Class[_ <: T]] = {

    val reflects = new Reflections(name)

    val data = reflects.getSubTypesOf(underlying).asScala.toSeq
    val classes = data.filter(t => {
      !t.isInterface && t.getConstructors.length > 0 &&
        t.getConstructors()(0).getParameterTypes.length >= params
      //      &&
      //        !t.getConstructors()(0).getParameterTypes()(0).isInterface
    }).map(t => {
      (t.getConstructors()(0).getParameterTypes()(0), t)
    })
    filter match {
      case Some(t) =>
        classes.filter(_._2.getName.contains(t)).toMap
      case _ =>
        classes.toMap
    }
  }

  final def subClassList[T](underlying: Class[T], name: String = "com.haima"): List[Class[_ <: T]] = {
    val reflects = new Reflections(name)
    val data = reflects.getSubTypesOf(underlying).asScala.toSeq
    data.filter(t => !t.isInterface).toList
  }

  object ParamType {
    final val regex: Regex = """.*\(\w+: ([^,\)]*).*""".r

    def unapply(str: String): Option[Class[_]] = {
      regex.unapplySeq(str) match {
        case Some(name :: Nil) =>
          Try(Class.forName(name)).toOption
        case _ =>
          None
      }
    }
  }
}