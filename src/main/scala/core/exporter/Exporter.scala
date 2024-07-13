package org.unibo.scooby
package core.exporter
import core.scraper.{DataResult, Result}

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, StandardOpenOption}
import scala.util.Try

/**
 * Type alias for a function that defines how to export a `Result`.
 */
type ExportingBehavior[A] = Result[A] => Unit

/**
 * Type alias for a function that defines how to aggregate two `Result` instances into a single `Result`.
 */
type AggregationBehavior[A] = (Result[A], Result[A]) => Result[A]

/**
 * Type alias for a function that defines how to format a `Result` for exporting.
 */
type FormattingBehavior[A] = Result[A] => String

/**
 * Defines commands that can be handled by the `Exporter` actor.
 */
enum ExporterCommands:
  /**
   * Command to export a result.
   *
   * @tparam A The type of data in the result.
   * @param result The result to be exported.
   */
  case Export[A](result: Result[A]) 
  /**
   * Command to signal the end of computation, indicating that no more results will be sent.
   */
  case SignalEnd()
  
/**
 * Companion object for the `Exporter` actor, providing factory methods for creating different types of exporting behaviors.
 */
object Exporter:
  import ExporterCommands._

  /**
   * Creates a behavior for streaming export.
   *
   * @tparam A The type of data in the result.
   * @param exportingFunction The function that defines how to export the result.
   * @return A behavior for handling `ExporterCommands`.
   */
  def stream[A](exportingFunction: ExportingBehavior[A]): Behavior[ExporterCommands] =
    Behaviors.setup : context =>
      Behaviors.receiveMessage :
        case Export(result: Result[A]) =>
          exportingFunction(result)
          Behaviors.same
        case SignalEnd() =>
          context.log.error("Stream Exporter can only accept stream results")
          Behaviors.same

  /**
   * Creates a behavior for folding (aggregating) export.
   *
   * @tparam A The type of data in the result.
   * @param result            The initial result to start with.
   * @param exportingFunction The function that defines how to export the aggregated result.
   * @param aggregation       The function that defines how to aggregate results.
   * @return A behavior for handling `ExporterCommands`.
   */
  def fold[A](result: Result[A])
             (exportingFunction: ExportingBehavior[A])
             (aggregation: AggregationBehavior[A]): Behavior[ExporterCommands] =
    Behaviors.setup : context =>
      Behaviors.receiveMessage :
        case Export(newResult: Result[A]) =>
          fold(aggregation(result, newResult))(exportingFunction)(aggregation)
        case SignalEnd() =>
          exportingFunction(result)
          Behaviors.same

  /**
   * Creates a behavior for batching export.
   *
   * @tparam A The type of data in the result.
   * @param exportingFunction The function that defines how to export the result.
   * @param aggregation       The function that defines how to aggregate results.
   * @return A behavior for handling `ExporterCommands`.
   */
  def batch[A](exportingFunction: ExportingBehavior[A])
              (aggregation: AggregationBehavior[A]): Behavior[ExporterCommands] =
    fold(Result.empty[A])(exportingFunction)(aggregation)
  
  /**
   * Contains predefined exporting behaviors.
   */
  object ExportingBehaviors:

    /**
     * Creates an exporting behavior that writes results to a file.
     *
     * @tparam A The type of data in the result.
     * @param filePath The path to the file where results will be written.
     * @param format   The formatting behavior to use for converting results to strings.
     * @return An exporting behavior.
     */
    def writeOnFile[A](filePath: Path, format: FormattingBehavior[A] = Formats.string): ExportingBehavior[A] =
      (result: Result[A]) =>
      Try :
        val writer = Files.newBufferedWriter(
          filePath,
          StandardCharsets.UTF_8,
          StandardOpenOption.CREATE,
          StandardOpenOption.APPEND
        )
        val content = format(result)
        (writer, content)

      .toEither match
        case Left(exception) => println(f"Error while writing to file: $exception")
        case Right(writer, content) =>
          writer.write(content)
          writer.close()

    /**
     * Creates an exporting behavior that writes results to the console.
     *
     * @tparam A The type of data in the result.
     * @param format The formatting behavior to use for converting results to strings.
     * @return An exporting behavior.
     */
    def writeOnConsole[A](format: FormattingBehavior[A] = Formats.string) : ExportingBehavior[A] =
      (result: Result[A]) => println(format(result))

  /**
   * Contains predefined aggregation behaviors.
   */
  object AggregationBehaviors:
    /**
     * The default aggregation behavior that simply aggregates two results.
     *
     * @tparam A The type of data in the result.
     * @return An aggregation behavior.
     */
    def default[A]: AggregationBehavior[A] = (res1: Result[A], res2: Result[A]) => res1.aggregate(res2)

  /**
   * Contains predefined formatting behaviors.
   */
  object Formats:
    /**
     * A simple string formatting behavior that converts the result's data to a string.
     *
     * @tparam A The type of data in the result.
     * @return A formatting behavior.
     */
    def string[A]: FormattingBehavior[A] = (result: Result[A]) => result.data.toString() + System.lineSeparator()