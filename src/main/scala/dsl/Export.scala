package org.unibo.scooby
package dsl

import core.exporter.Exporter.{AggregationBehaviors, ExportingBehaviors, Formats}
import core.exporter.FormattingBehavior
import core.scooby.SingleExporting
import core.scooby.SingleExporting.{BatchExporting, StreamExporting}
import core.scraper.Result
import dsl.DSL.ConfigurationBuilder

import _root_.dsl.syntax.catchRecursiveCtx
import monocle.syntax.all.*

import java.nio.file.Path

object Export:
  import Context.*
  import Context.Batch.*
  import Context.Stream.*
  export ExportOps.SafeOps.*
  export ExportOps.FormatType.*
  export ExportOps.{exports, Streaming, Batch, strategy, aggregate, results, ToFile, ToConsole}

  object ExportOps:

    /**
     * Build the [[Exporter]] context to set up the exporting strategies.
     * @param builder the [[ConfigurationBuilder]] containing all application parameters.
     * @tparam T the [[Result]]'s type.
     * @return the [[ExportContext]] built.
     */
    def exports[T](using builder: ConfigurationBuilder[T]): ExportContext[T] =
      ExportContext[T](builder)

    /**
     * Build the [[Exporter]] stream context.
     * @param context the [[StrategiesContext]] containing exporting strategies.
     * @tparam T the [[Result]]'s type.
     * @return the [[StreamExportationContext]] built.
     */
    def Streaming[T](using context: StrategiesContext[T]): StreamExportationContext[T] = 
      StreamExportationContext[T](context)

    /**
     * Build the [[Exporter]] batch context.
     * @param context the [[StrategiesContext]] containing exporting strategies.
     * @tparam T the [[Result]]'s type.
     * @return the [[BatchExportationContext]] built.
     */
    def Batch[T](using context: StrategiesContext[T]): BatchExportationContext[T] = 
      BatchExportationContext[T](context)

    /**
     * Build the [[Exporter]] batch's strategy context.
     * @param context the [[BatchSettingContext]] containing settings of batch technique.
     * @tparam T the [[Result]]'s type.
     * @return the [[BatchStrategyContext]]
     */
    def strategy[T](using context: BatchSettingContext[T]): BatchStrategyContext[T] = 
      BatchStrategyContext[T](context)

    /**
     * Build the [[Exporter]] batch's aggregate context.
     * @param context the [[BatchSettingContext]] containing settings of batch technique.
     * @tparam T the [[Result]]'s type.
     * @return the [[BatchStrategyContext]]
     */
    def aggregate[T](using context: BatchSettingContext[T]): BatchAggregateContext[T] =
      BatchAggregateContext[T](context)

    /**
     * Retrieve from the context an [[Iterable]].
     * @param context the context used to retrieve the [[Iterable]].
     * @tparam T the [[Iterable]] type.
     * @return the [[Iterable]] retrieved.
     */
    def results[T](using context: Iterable[T]): Iterable[T] = context

    /**
     * Supported output devices.
     */
    enum ExportSupport:
      case Console
      case File(path: String)

    /**
     * Build the exporting function as write on [[File]].
     * @param path the path to retrieve the [[File]].
     * @param exporter context used to retrieve type of [[Result]].
     * @tparam T the type of [[Result]] to export.
     * @return the [[WriteOnOutput]] with configuration built.
     */
    infix def ToFile[T](path: String)(using exporter: ExportStrategyContext[T]): WriteOnOutput[T] =
      WriteOnOutput[T](exporter, ExportSupport.File(path))

    /**
     * Build the exporting function as write on [[Console]].
     * @param exporter context used to retrieve type of [[Result]].
     * @tparam T the type of [[Result]] to export.
     * @return the [[WriteOnOutput]] with configuration built.
     */
    infix def ToConsole[T](using exporter: ExportStrategyContext[T]): WriteOnOutput[T] =
      WriteOnOutput[T](exporter, ExportSupport.Console)

    /**
     * Export format types supported.
     */
    enum FormatType:
      case Text
      case Json
    
    object SafeOps:
      import UnsafeOps.*
      extension [T](x: Iterable[T])
        /**
         * Context used to apply the [[Exporter]] consume function.
         * @param f the consume function to apply.
         */
        inline infix def output(f: ExportStrategyContext[T] ?=> Iterable[T] => Unit): Unit  =
          catchRecursiveCtx[ExportStrategyContext[?]]("output")
          x.outputOp(f)

    private[Export] object UnsafeOps:

      extension [T](x: Iterable[T])
        /**
          * Unsafe version of the one inside [[org.unibo.scooby.dsl.Export.ExportOps.SafeOps]]
          * @param f the consume function to apply.
          */
        def outputOp(f: ExportStrategyContext[T] ?=> Iterable[T] => Unit): Unit  =
          given ExportStrategyContext[T] = ExportStrategyContext[T]()
          f(x)

  private[Export] object Context:
    import Export.ExportOps.{ExportSupport, FormatType}
    /**
     * Context used to parse the exporting strategies given in configuration.
     * @param builder the [[ConfigurationBuilder]] containing all application parameters.
     * @tparam T the [[Result]]'s type.
     */
    case class ExportContext[T](builder: ConfigurationBuilder[T]):

      /**
       * Builder used to summon the [[StrategiesContext]] containing exporting strategies and parse them in application
       * configuration.
       * @param init function which set the exporting strategies in [[StrategiesContext]].
       */
      inline infix def apply(init: StrategiesContext[T] ?=> Unit): Unit =
        catchRecursiveCtx[StrategiesContext[?]]("export")
        visitCtxUnsafe(init)

      /**
       * Unsafe version of [[ExportContext.apply]]
       * @param init function which set the exporting strategies in [[StrategiesContext]].
       */
      private def visitCtxUnsafe(init: StrategiesContext[T] ?=> Unit): Unit =
        given context: StrategiesContext[T] = StrategiesContext[T](Seq.empty[SingleExporting[T]])
        init
        builder.configuration = builder.configuration
          .focus(_.exporterConfiguration.exportingStrategies).replace(context.exportingStrategies)

    /**
     * Context containing all exporting strategies.
     * @param exportingStrategies the [[Seq]] of [[SingleExporting]] strategies.
     * @tparam T the [[Result]]'s type.
     */
    case class StrategiesContext[T](var exportingStrategies: Seq[SingleExporting[T]])

    /**
     * Context used to define the exporting function.
     * @param context the export context used to retrieve type of [[Result]].
     * @param support the [[ExportSupport]] to use.
     * @tparam T the [[Result]]'s type.
     */
    case class WriteOnOutput[T](context: ExportStrategyContext[T], support: ExportSupport):

      /**
       * Build the export function parsing the [[FormatType]] to use.
       * @param strategy the [[FormatType]] to format the [[Result]].
       * @return the exporting function.
       */
      infix def withFormat(strategy: FormatType): Iterable[T] => Unit =
        (it: Iterable[T]) =>
          val format: FormattingBehavior[T] = strategy match
            case FormatType.Text => Formats.string
            case FormatType.Json => Formats.string // TODO once implemented Json export, parse it

          support match
            case ExportSupport.File(path: String) =>
              ExportingBehaviors.writeOnFile(Path.of(path), format)(Result(it))
            case ExportSupport.Console =>
              println(it.mkString("\n"))

    /**
     * The context used to retrieve type of [[Result]].
     * @tparam T the [[Result]]'s type.
     */
    case class ExportStrategyContext[T]()

    object Batch:

      /**
       * The exporter batch technique's context.
       * @param context the context used to set the [[BatchExporting]] configuration.
       * @tparam T the [[Result]]'s type.
       */
      case class BatchExportationContext[T](context: StrategiesContext[T]):

        /**
         * Builder used to set the [[BatchExporting]] configuration.
         * @param init the function used to set the [[BatchExporting]] configuration.
         */
        inline infix def apply(init: BatchSettingContext[T] ?=> Unit): Unit =
          catchRecursiveCtx[BatchSettingContext[?]]("Batch")
          visitCtxUnsafe(init)

        /**
         * Unsafe version of [[BatchExportationContext.apply]].
         * @param init the function used to set the [[BatchExporting]] configuration.
         */
        private def visitCtxUnsafe(init: BatchSettingContext[T] ?=> Unit): Unit =
          given batchStrategyContext: BatchSettingContext[T] =
            BatchSettingContext[T](ExportingBehaviors.writeOnConsole(Formats.string), AggregationBehaviors.default)
          init
          context.exportingStrategies = Seq(BatchExporting(
            batchStrategyContext.policy,
            batchStrategyContext.aggregation
          ))

      /**
       * Context containing [[BatchExporting]] configuration.
       * @param policy the function applied once all [[Result]]s are received.
       * @param aggregation the function used to aggregate all [[Result]]s.
       * @tparam T the [[Result]]'s type.
       */
      case class BatchSettingContext[T](
        var policy: Result[T] => Unit, 
        var aggregation: (Result[T], Result[T]) => Result[T])

      /**
       * Context to set batch policy function.
       * @param context the context used to set the [[BatchExporting]] policy.
       * @tparam T the [[Result]]'s type.
       */
      case class BatchStrategyContext[T](context: BatchSettingContext[T]):
        inline infix def apply(init: Iterable[T] ?=> Unit): Unit =
          catchRecursiveCtx[Iterable[?]]("strategy")
          context.policy = (res: Result[T]) => 
            given Iterable[T] = res.data
            init

      /**
       * Context to set batch aggregate function.
       * @param context the context used to set the [[BatchExporting]] aggregation.
       * @tparam T the [[Result]]'s type.
       */
      case class BatchAggregateContext[T](context: BatchSettingContext[T]):
        infix def apply(init: (Iterable[T], Iterable[T]) => Iterable[T]): Unit = 
          context.aggregation = (res1: Result[T], res2: Result[T]) => Result(init(res1.data, res2.data))
          
    object Stream:

      /**
       * The exporter stream technique's context.
       * @param context the context used to set the [[StreamExporting]] configuration.
       * @tparam T the [[Result]]'s type.
       */
      case class StreamExportationContext[T](context: StrategiesContext[T]):
        inline infix def apply(init: Iterable[T] ?=> Unit): Unit =
          catchRecursiveCtx[Iterable[?]]("Streaming")
          context.exportingStrategies = context.exportingStrategies :+ StreamExporting[T](
          (res: Result[T]) => 
            given Iterable[T] = res.data
            init
          )
