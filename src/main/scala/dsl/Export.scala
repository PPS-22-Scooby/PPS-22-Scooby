package org.unibo.scooby
package dsl

import core.scooby.SingleExporting.{BatchExporting, StreamExporting}
import core.scraper.Result
import dsl.DSL.ConfigurationBuilder

import monocle.syntax.all.*
import scala.util.Try
import core.scooby.SingleExporting
import org.unibo.scooby.core.exporter.Exporter.AggregationBehaviors
import org.unibo.scooby.core.exporter.Exporter.ExportingBehaviors
import org.unibo.scooby.core.exporter.Exporter.Formats
import org.unibo.scooby.dsl.DSL.ScrapingResultSetting

object Export:

  case class ExportationContext[T](builder: ConfigurationBuilder[T]):
    given ConfigurationBuilder[T] = builder
    infix def as(init: ExportationOpContext[T] ?=> Unit)(using ConfigurationBuilder[T]): Unit =
      given context: ExportationOpContext[T] = ExportationOpContext[T](Seq.empty[SingleExporting[T]])
      init
      builder.configuration = builder.configuration
        .focus(_.exporterConfiguration.exportingStrategies)      .replace(context.exportingStrategies)


  def exports[T](using builder: ConfigurationBuilder[T]): ExportationContext[T] =
    ExportationContext[T](builder)

  def results[T](using context: Iterable[T]): Iterable[T] = context.asInstanceOf[Iterable[T]]


  case class ExportationOpContext[T](var exportingStrategies: Seq[SingleExporting[T]])
    
  def stream[T](init: Iterable[T] ?=> Unit)
    (using builder: ExportationOpContext[T]): Unit =
    builder.exportingStrategies = builder.exportingStrategies :+ StreamExporting[T](
      (res: Result[T]) => 
        given Iterable[T] = res.data
        init
      )
  
  case class BatchContext[T](var policy: Result[T] => Unit, var aggregation: (Result[T], Result[T]) => Result[T])

  def aggregation[T](init: (Iterable[T], Iterable[T]) => Iterable[T])(using context: BatchContext[T]): Unit = 
    context.aggregation = (res1: Result[T], res2: Result[T]) => Result(init(res1.data, res2.data))
    
        
  def strategy[T](init: Iterable[T] ?=> Unit)(using context: BatchContext[T]): Unit = 
    context.policy = (res: Result[T]) => 
      given Iterable[T] = res.data
      init
    
    
  def batch[T](init: BatchContext[T] ?=> Unit)(using builder: ExportationOpContext[T]): Unit =
    given context: BatchContext[T] = BatchContext[T](
      (res: Result[T]) => println(res), (res1: Result[T], res2: Result[T]) => Result(Iterable.empty[T])
    )
    init
    
    builder.exportingStrategies = builder.exportingStrategies :+ BatchExporting(
      context.policy,
      context.aggregation
    )
    
