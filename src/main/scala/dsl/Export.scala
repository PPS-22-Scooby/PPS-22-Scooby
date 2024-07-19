package org.unibo.scooby
package dsl

object Export:
  
  
  
  infix def exports[T](init: ExportContext[T] ?=> Unit): ExportContext[T] =
    ???

  case class ExportContext[T]()



