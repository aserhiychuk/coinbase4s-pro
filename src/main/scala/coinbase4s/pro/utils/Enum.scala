package coinbase4s.pro.utils

import scala.reflect.runtime.universe._

abstract class Enum[T](implicit valueTypeTag: TypeTag[T]) {
  protected lazy val values: Seq[T] = findValues

  def apply(value: String): T = values.find(_.toString == value) match {
    case Some(result) => result
    case None => throw new NoSuchElementException(s"No value found for '$value'")
  }

  protected def findValues: Seq[T] = {
    val universeMirror = runtimeMirror(getClass.getClassLoader)
    val valueClass = valueTypeTag.tpe.typeSymbol.asClass
    valueClass
      .knownDirectSubclasses
      .map { subClass =>
        universeMirror.reflectModule(subClass.asClass.module.asModule).instance.asInstanceOf[T]
      }
      .toSeq
  }
}
