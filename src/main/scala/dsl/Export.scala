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
    infix def apply(init: ExportationOpContext[T] ?=> Unit)(using ConfigurationBuilder[T]): Unit =
      given context: ExportationOpContext[T] = ExportationOpContext[T](Seq.empty[SingleExporting[T]])
      init
      builder.configuration = builder.configuration
        .focus(_.exporterConfiguration.exportingStrategies)      .replace(context.exportingStrategies)


  def exports[T](using builder: ConfigurationBuilder[T]): ExportationContext[T] =
    ExportationContext[T](builder)

  def results[T](using context: Iterable[T]): Iterable[T] = context.asInstanceOf[Iterable[T]]


  case class ExportationOpContext[T](var exportingStrategies: Seq[SingleExporting[T]])
  
  
  case class BatchContext[T](var policy: Result[T] => Unit, var aggregation: (Result[T], Result[T]) => Result[T])

  case class BatchStrategyContext[T]():
    infix def apply(init: Iterable[T] ?=> Unit)(using context: BatchContext[T]): Unit = 
      context.policy = (res: Result[T]) => 
        given Iterable[T] = res.data
        init

  case class BatchAggregateContext[T]():
    infix def apply(init: (Iterable[T], Iterable[T]) => Iterable[T])(using context: BatchContext[T]): Unit = 
      context.aggregation = (res1: Result[T], res2: Result[T]) => Result(init(res1.data, res2.data))
        
  def strategy[T](using BatchContext[T]): BatchStrategyContext[T] = 
    BatchStrategyContext[T]() 
    
  def aggregate[T](using BatchContext[T]): BatchAggregateContext[T] = BatchAggregateContext[T]()

    
  case class BatchExportationContext[T]():
    infix def apply(init: BatchContext[T] ?=> Unit)(using builder: ExportationOpContext[T]): Unit =
      given context: BatchContext[T] = BatchContext[T](
      (res: Result[T]) => println(res), AggregationBehaviors.default)
      init
    
      builder.exportingStrategies = Seq(BatchExporting(
        context.policy,
        context.aggregation
      ))

  case class StreamExportationContext[T]():
    infix def apply(init: Iterable[T] ?=> Unit)(using builder: ExportationOpContext[T]): Unit =
      builder.exportingStrategies = builder.exportingStrategies :+ StreamExporting[T](
      (res: Result[T]) => 
        given Iterable[T] = res.data
        init
      )

  def Streaming[T](using ExportationOpContext[T]): StreamExportationContext[T] = StreamExportationContext[T]()    
  def Batch[T](using ExportationOpContext[T]): BatchExportationContext[T] = BatchExportationContext[T]()
    
    
