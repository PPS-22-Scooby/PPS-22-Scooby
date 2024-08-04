package org.unibo.scooby
package dsl

import monocle.syntax.all.*
import org.unibo.scooby.core.exporter.Exporter.{AggregationBehaviors, ExportingBehaviors, Formats}
import org.unibo.scooby.core.exporter.FormattingBehavior
import org.unibo.scooby.core.scooby.SingleExporting
import org.unibo.scooby.core.scooby.SingleExporting.{BatchExporting, StreamExporting}
import org.unibo.scooby.core.scraper.Result
import org.unibo.scooby.dsl.DSL.ConfigurationWrapper
import org.unibo.scooby.dsl.syntax.catchRecursiveCtx
import play.api.libs.json.Writes

import java.nio.file.Path

/**
 * Collection of DSL operators to customize the [[Exporter]] properties of Scooby application
 *
 * Example usage:
 * {{{
 *   exports:
 *    batch:
 *      strategy:
 *        results get tag output:
 *          toConsole withFormat text
 *          // or toFile("outputFileName") withFormat text
 *
 *      aggregate:
 *        _ ++ _
 *
 *    streaming:
 *      results get tag output:
 *        toConsole withFormat text
 * }}}
 */
object Export:
  import Context.*
  import Context.Batch.*
  import Context.Stream.*
  export ExportOps.SafeOps.*
  export ExportOps.{exports, streaming, batch, strategy, aggregate, results, toFile, toConsole, text, json}
  export ExportOps.FileAction.*

  object ExportOps:

    /**
     * Build the [[Exporter]] context to set up the exporting strategies.
 *
     * @param builder the [[ConfigurationWrapper]] containing all application parameters.
     * @tparam T the [[Result]]'s type.
     * @return the [[ExportContext]] built.
     */
    def exports[T](using builder: ConfigurationWrapper[T]): ExportContext[T] =
      ExportContext[T](builder)

    /**
     * Build the [[Exporter]] stream context.
     *
     * @param context the [[StrategiesContext]] containing exporting strategies.
     * @tparam T the [[Result]]'s type.
     * @return the [[StreamStrategyContext]] built.
     */
    def streaming[T](using context: StrategiesContext[T]): StreamStrategyContext[T] =
      StreamStrategyContext[T](context)

    /**
     * Build the [[Exporter]] batch context.
     * @param context the [[StrategiesContext]] containing exporting strategies.
     * @tparam T the [[Result]]'s type.
     * @return the [[BatchExportationContext]] built.
     */
    def batch[T](using context: StrategiesContext[T]): BatchExportationContext[T] =
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
     * Action to perform on file (overwrite or append) when exporting
     */
    enum FileAction:
      case Append
      case Overwrite

    /**
     * Build the exporting function as write on [[File]], overwriting it if already existing.
     * @param path     the path to retrieve the [[File]].
     * @param exporter context used to retrieve type of [[Result]].
     * @tparam T the type of [[Result]] to export.
     * @return the [[WriteOnOutput]] with configuration built.
     */
    infix def toFile[T](path: String)
                       (using exporter: ExportStrategyContext[T]): WriteOnOutput[T] =
      toFile(path, FileAction.Overwrite)

    /**
     * Build the exporting function as write on [[File]].
     * @param path     the path to retrieve the [[File]].
     * @param exporter context used to retrieve type of [[Result]].
     * @param fileAction action to perform on the file (append or overwrite if existing)
     * @tparam T the type of [[Result]] to export.
     * @return the [[WriteOnOutput]] with configuration built.
     */
    infix def toFile[T](path: String, fileAction: FileAction)
                       (using exporter: ExportStrategyContext[T]): WriteOnOutput[T] =
      WriteOnOutput[T](exporter, ExportSupport.File(path), fileAction)

    /**
     * Build the exporting function as write on [[Console]].
     * @param exporter context used to retrieve type of [[Result]].
     * @tparam T the type of [[Result]] to export.
     * @return the [[WriteOnOutput]] with configuration built.
     */
    infix def toConsole[T](using exporter: ExportStrategyContext[T]): WriteOnOutput[T] =
      WriteOnOutput[T](exporter, ExportSupport.Console)

    /**
     * Export format types supported.
     */
    enum FormatType:
      private[Export] case Text
      private[Export] case Json[T](writes: Writes[T])

    /**
     * Utility keyword to obtain the Text export strategy
     * @param context context withing this keyword can be used
     * @tparam T type of the scrape result
     * @return the [[FormatType]] text
     */
    def text[T](using context: ExportStrategyContext[T]): FormatType = FormatType.Text

    /**
     * Utility keyword to obtain the Json export strategy
     * @param context context withing this keyword can be used
     * @param writer JSON writer of the Play library
     * @tparam T type of the scrape result
     * @return the [[FormatType]] text
     */
    def json[T](using context: ExportStrategyContext[T])(using writer: Writes[T]): FormatType =
      FormatType.Json(writer)


    /**
     * Type alias representing the "output" section under "strategy"
     * @tparam T type of results returned by the scraping
     */
    private type OutputDefinitionScope[T] = ExportStrategyContext[T] ?=> Iterable[T] => Unit

    /**
     * Collection of operators for "export" part of the DSL, performing also syntax checks.
     */
    object SafeOps:
      import UnsafeOps.*

      extension [T](x: Iterable[T])

        /**
         * Context used to apply the [[Exporter]] consume function.
         * @param f the consume function to apply.
         */
        inline infix def output(f: OutputDefinitionScope[T]): Unit =
          catchRecursiveCtx[ExportStrategyContext[?]]("output")
          x.outputOp(f)

    /**
     * Collection of unsafe operators, containing the unsafe versions of the one inside [[SafeOps]]
     */
    private[Export] object UnsafeOps:

      extension [T](x: Iterable[T])

        /**
         * Unsafe version of the one inside [[org.unibo.scooby.dsl.Export.ExportOps.SafeOps]]
         * @param f the consume function to apply.
         */
        def outputOp(f: OutputDefinitionScope[T]): Unit  =
          given ExportStrategyContext[T] = ExportStrategyContext[T]()
          f(x)

  /**
   * Type alias representing the "exports" section of the DSL
   * @tparam T type of the results returned by the scraping
   */
  private type ExportDefinitionScope[T] = StrategiesContext[T] ?=> Unit

  /**
   * Collection of contexts used inside the "export" part of the DSL.
   */
  private[Export] object Context:
    import Export.ExportOps.{ExportSupport, FormatType}
    import Export.ExportOps.FileAction

    /**
     * Context used to parse the exporting strategies given in configuration.
 *
     * @param builder the [[ConfigurationWrapper]] containing all application parameters.
     * @tparam T the [[Result]]'s type.
     */
    case class ExportContext[T](builder: ConfigurationWrapper[T]):

      /**
       * Builder used to summon the [[StrategiesContext]] containing exporting strategies and parse them in application
       * configuration.
       * @param block function which set the exporting strategies in [[StrategiesContext]].
       */
      inline infix def apply(block: ExportDefinitionScope[T]): Unit =
        catchRecursiveCtx[StrategiesContext[?]]("export")
        visitCtxUnsafe(block)

      /**
       * Unsafe version of [[ExportContext.apply]]
       * @param block function which set the exporting strategies in [[StrategiesContext]].
       */
      private def visitCtxUnsafe(block: ExportDefinitionScope[T]): Unit =
        given context: StrategiesContext[T] = StrategiesContext[T](Seq.empty[SingleExporting[T]])
        block
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
     * @param fileAction specifies what to do in case the output is directed to a file (overwrite or append)
     * @tparam T the [[Result]]'s type.
     */
    case class WriteOnOutput[T](context: ExportStrategyContext[T], support: ExportSupport,
                                fileAction: FileAction = FileAction.Overwrite):

      /**
       * Build the export function parsing the [[FormatType]] to use.
       * @param strategy the [[FormatType]] to format the [[Result]].
       * @return the exporting function.
       */
      infix def withFormat(strategy: FormatType): Iterable[T] => Unit =
        (it: Iterable[T]) =>
          val format: FormattingBehavior[T] = strategy match
            case FormatType.Text => Formats.string
            case FormatType.Json(writer: Writes[T]) =>
              given Writes[T] = writer
              Formats.json
          val fileOverwrite = fileAction match
            case FileAction.Overwrite => true
            case FileAction.Append => false
          support match
            case ExportSupport.File(path: String) =>
              ExportingBehaviors.writeOnFile(Path.of(path), format, fileOverwrite)(Result(it))
            case ExportSupport.Console =>
              ExportingBehaviors.writeOnConsole(format)(Result(it))

    /**
     * The context used to retrieve type of [[Result]].
     * @tparam T the [[Result]]'s type.
     */
    case class ExportStrategyContext[T]()

    /**
     * Type alias representing <b>either</b>:
     *  - the "strategy" section under "Batch" part of the DSL
     *  - the "Streaming" section under "exports" part of the DSL
     * @tparam T type of the Results returned by the scraping
     */
    private type StrategyDefinitionScope[T] = Iterable[T] ?=> Unit

    object Batch:

      /**
       * Type alias representing the "Batch" section under the "exports" part of the DSL
       * @tparam T type of results returned by the scraping.
       */
      private type BatchDefinitionScope[T] = BatchSettingContext[T] ?=> Unit

      /**
       * The exporter batch technique's context.
       * @param context the context used to set the [[BatchExporting]] configuration.
       * @tparam T the [[Result]]'s type.
       */
      case class BatchExportationContext[T](context: StrategiesContext[T]):

        /**
         * Builder used to set the [[BatchExporting]] configuration.
         * @param block the function used to set the [[BatchExporting]] configuration.
         */
        inline infix def apply(block: BatchDefinitionScope[T]): Unit =
          catchRecursiveCtx[BatchSettingContext[?]]("batch")
          visitCtxUnsafe(block)

        /**
         * Unsafe version of [[BatchExportationContext.apply]].
         * @param block the function used to set the [[BatchExporting]] configuration.
         */
        private def visitCtxUnsafe(block: BatchDefinitionScope[T]): Unit =
          given batchStrategyContext: BatchSettingContext[T] = BatchSettingContext[T](
            ExportingBehaviors.writeOnConsole(Formats.string), AggregationBehaviors.default)
          block
          context.exportingStrategies ++= Seq(BatchExporting(
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
        inline infix def apply(block: StrategyDefinitionScope[T]): Unit =
          catchRecursiveCtx[Iterable[?]]("strategy")
          context.policy = (res: Result[T]) =>
            given Iterable[T] = res.data
            block

      /**
       * Context to set batch aggregate function.
       * @param context the context used to set the [[BatchExporting]] aggregation.
       * @tparam T the [[Result]]'s type.
       */
      case class BatchAggregateContext[T](context: BatchSettingContext[T]):
        infix def apply(block: (Iterable[T], Iterable[T]) => Iterable[T]): Unit =
          context.aggregation = (res1: Result[T], res2: Result[T]) => Result(block(res1.data, res2.data))

    object Stream:

      /**
       * The exporter stream technique's context.
       * @param context the context used to set the [[StreamExporting]] configuration.
       * @tparam T the [[Result]]'s type.
       */
      case class StreamStrategyContext[T](context: StrategiesContext[T]):
        inline infix def apply(block: StrategyDefinitionScope[T]): Unit =
          catchRecursiveCtx[Iterable[?]]("streaming")
          context.exportingStrategies = context.exportingStrategies :+ StreamExporting[T](
            (res: Result[T]) =>
              given Iterable[T] = res.data
              block
          )
