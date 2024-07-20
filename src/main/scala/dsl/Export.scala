package org.unibo.scooby
package dsl

import monocle.syntax.all.*
import org.unibo.scooby.core.exporter.Exporter.AggregationBehaviors

import core.scooby.SingleExporting.{BatchExporting, StreamExporting}
import core.scraper.Result
import dsl.DSL.ConfigurationBuilder
import core.scooby.SingleExporting
import org.unibo.scooby.core.exporter.Exporter.{ExportingBehaviors, Formats}

object Export:
  import Context.*
  import Context.Batch.* 
  import Context.Stream.*
  export ExportOps.*

  object ExportOps:
    def exports[T](using builder: ConfigurationBuilder[T]): ExportContext[T] =
      ExportContext[T](builder)

    def Streaming[T](using context: StrategiesContext[T]): StreamExportationContext[T] = 
      StreamExportationContext[T](context)    
    def Batch[T](using context: StrategiesContext[T]): BatchExportationContext[T] = 
      BatchExportationContext[T](context)

    def strategy[T](using context: BatchSettingContext[T]): BatchStrategyContext[T] = 
      BatchStrategyContext[T](context)    
    def aggregate[T](using context: BatchSettingContext[T]): BatchAggregateContext[T] = 
      BatchAggregateContext[T](context)

    def results[T](using context: Iterable[T]): Iterable[T] = context.asInstanceOf[Iterable[T]]


  object Context:

    case class ExportContext[T](builder: ConfigurationBuilder[T]):
      infix def apply(init: StrategiesContext[T] ?=> Unit): Unit =
        given context: StrategiesContext[T] = StrategiesContext[T](Seq.empty[SingleExporting[T]])
        init
        builder.configuration = builder.configuration
          .focus(_.exporterConfiguration.exportingStrategies)      .replace(context.exportingStrategies)


    case class StrategiesContext[T](var exportingStrategies: Seq[SingleExporting[T]])

    object Batch:
      case class BatchExportationContext[T](context: StrategiesContext[T]):
        infix def apply(init: BatchSettingContext[T] ?=> Unit): Unit =
          given batchStrategyContext: BatchSettingContext[T] = 
            BatchSettingContext[T](ExportingBehaviors.writeOnConsole(Formats.string), AggregationBehaviors.default)
          
          init
          context.exportingStrategies = Seq(BatchExporting(
            batchStrategyContext.policy,
            batchStrategyContext.aggregation
          ))

      case class BatchSettingContext[T](
        var policy: Result[T] => Unit, 
        var aggregation: (Result[T], Result[T]) => Result[T])

      case class BatchStrategyContext[T](context: BatchSettingContext[T]):
        infix def apply(init: Iterable[T] ?=> Unit): Unit = 
          context.policy = (res: Result[T]) => 
            given Iterable[T] = res.data
            init

      case class BatchAggregateContext[T](context: BatchSettingContext[T]):
        infix def apply(init: (Iterable[T], Iterable[T]) => Iterable[T]): Unit = 
          context.aggregation = (res1: Result[T], res2: Result[T]) => Result(init(res1.data, res2.data))
          
    object Stream:
      case class StreamExportationContext[T](context: StrategiesContext[T]):
        infix def apply(init: Iterable[T] ?=> Unit): Unit =
          context.exportingStrategies = context.exportingStrategies :+ StreamExporting[T](
          (res: Result[T]) => 
            given Iterable[T] = res.data
            init
          )
