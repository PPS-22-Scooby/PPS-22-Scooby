package org.unibo.scooby
package dsl

import monocle.syntax.all.*
import core.exporter.FormattingBehavior
import core.exporter.Exporter.{AggregationBehaviors, ExportingBehaviors, Formats}
import core.scooby.SingleExporting.{BatchExporting, StreamExporting}
import core.scraper.Result
import dsl.DSL.ConfigurationBuilder
import core.scooby.SingleExporting
import utility.document.html.HTMLElement

import scala.annotation.targetName
import java.nio.file.Path

object Export:
  import Context.*
  import Context.Batch.* 
  import Context.Stream.*
  export ExportOps.*
  export ExportOps.Strategy.*

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

    def results[T](using context: Iterable[T]): Iterable[T] = context

    def tag: HTMLElement => String = _.tag
    def text: HTMLElement => String = _.text
    def outerText: HTMLElement => String = _.outerHtml
    def attr: String => HTMLElement => String = attribute => _.attr(attribute)

    enum ExportSupport:
      case Console
      case File(path: String)

    infix def File[T](path: String)(using exporter: ExportStrategyContext[T]): WriteOnOutput[T] =
      WriteOnOutput[T](exporter, ExportSupport.File(path))

    infix def Console[T](using exporter: ExportStrategyContext[T]): WriteOnOutput[T] =
      WriteOnOutput[T](exporter, ExportSupport.Console)

    enum Strategy:
      case Text
      case Json
    
    extension [T](x: Iterable[T])
      infix def outputTo(f: ExportStrategyContext[T] ?=> Iterable[T] => Unit): Unit  =
        given ExportStrategyContext[T] = ExportStrategyContext[T]()
        f(x)

      infix def get[A](f: T => A): Iterable[A] = x.map(f)
      @targetName("export")
      infix inline def >>(f: Iterable[T] => Unit): Unit = f(x)

  object Context:

    case class ExportContext[T](builder: ConfigurationBuilder[T]):
      infix def apply(init: StrategiesContext[T] ?=> Unit): Unit =
        given context: StrategiesContext[T] = StrategiesContext[T](Seq.empty[SingleExporting[T]])
        init
        builder.configuration = builder.configuration
          .focus(_.exporterConfiguration.exportingStrategies)      .replace(context.exportingStrategies)


    case class StrategiesContext[T](var exportingStrategies: Seq[SingleExporting[T]])

    import ExportOps.ExportSupport

    case class WriteOnOutput[T](context: ExportStrategyContext[T], support: ExportSupport):

      infix def asStrategy(strategy: Strategy): Iterable[T] => Unit =
        (it: Iterable[T]) =>
          val format: FormattingBehavior[T] = strategy match
            case Strategy.Text => Formats.string
            case Strategy.Json => Formats.string // TODO once implemented Json export, parse it

          support match
            case ExportSupport.File(path: String) =>
              ExportingBehaviors.writeOnFile(Path.of(path), format)(Result(it))
            case ExportSupport.Console =>
              println(it.mkString("\n"))

    case class ExportStrategyContext[T]()
      
    case class ExportToContext[T](it: Iterable[T]):
      def apply(f: ExportStrategyContext[T] ?=> Iterable[T] => Unit): Unit =
        given ExportStrategyContext[T] = ExportStrategyContext[T]()
        f(it)

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
