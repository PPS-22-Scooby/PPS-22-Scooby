package org.unibo.scooby
package dsl

import dsl.DSL.ConfigurationBuilder

object Export:

  case class ExportationContext[T](builder: ConfigurationBuilder[T]):
    infix def as(init: Iterable[T] ?=> Unit): Unit = ???

  infix def exports[T](using builder: ConfigurationBuilder[T]): ExportationContext[T] =
    ExportationContext[T](builder)

  case class ExportContext[T]()

  def results[T](using context: Iterable[T]): Iterable[T] = context.asInstanceOf[Iterable[T]]





