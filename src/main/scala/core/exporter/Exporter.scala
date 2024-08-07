package org.unibo.scooby
package core.exporter

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import org.unibo.scooby.core.scooby.ScoobyCommand
import org.unibo.scooby.core.scooby.ScoobyCommand.ExportFinished
import play.api.libs.json.Writes
import org.unibo.scooby.utility.result.Result

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
   * @param replyTo Actor to answer to after exporting has finished
   */
  case SignalEnd(replyTo: ActorRef[ScoobyCommand])


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
   * @return A [[Behavior]] for handling `ExporterCommands`.
   */
  def stream[A](exportingFunction: ExportingBehavior[A]): Behavior[ExporterCommands] =
    Behaviors.setup : context =>
      Behaviors.receiveMessage :
        case Export(result: Result[A]) =>
          Try:
            exportingFunction(result)
          .fold(e => println(s"An error occurred while exporting in stream config: $e"), identity)
          Behaviors.same
        case SignalEnd(replyTo) =>
          context.log.warn("Ignoring batch results inside Stream exporter")
          replyTo ! ExportFinished
          Behaviors.stopped

  /**
   * Creates a behavior for folding (aggregating) export.
   *
   * @tparam A The type of data in the result.
   * @param result            The initial result to start with.
   * @param exportingFunction The function that defines how to export the aggregated result.
   * @param aggregation       The function that defines how to aggregate results.
   * @return A [[Behavior]] for handling `ExporterCommands`.
   */
  def fold[A](result: Result[A])
             (exportingFunction: ExportingBehavior[A])
             (aggregation: AggregationBehavior[A]): Behavior[ExporterCommands] =
    Behaviors.setup : context =>
      Behaviors.receiveMessage :
        case Export(newResult: Result[A]) =>
          fold(aggregation(result, newResult))(exportingFunction)(aggregation)
        case SignalEnd(replyTo) =>
          Try:
            exportingFunction(result)
          .fold(e => println(s"An error occurred while exporting in batch config: $e"), identity)
          replyTo ! ExportFinished
          Behaviors.stopped

  /**
   * Creates a behavior for batching export.
   *
   * @tparam A The type of data in the result.
   * @param exportingFunction The function that defines how to export the result.
   * @param aggregation       The function that defines how to aggregate results.
   * @return A [[Behavior]] for handling `ExporterCommands`.
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
    def writeOnFile[A](filePath: Path,
                       format: FormattingBehavior[A] = Formats.string,
                       overwrite: Boolean = true): ExportingBehavior[A] =
      (result: Result[A]) =>
        Try :
          val writer = Files.newBufferedWriter(
            filePath,
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            if overwrite then StandardOpenOption.TRUNCATE_EXISTING else StandardOpenOption.APPEND,
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

    /**
     * Converts a `Result[A]` to a JSON string representation.
     * This function uses a given `Writes[Iterable[A]]` to serialize the `Result`'s data into JSON format.
     * The `Writes` typeclass is provided by Play Framework's JSON library and must be available in the scope
     * where this function is used. This allows for flexible and type-safe JSON serialization of various data types.
     *
     * @tparam A The type of data contained in the `Result`.
     * @param writer Implicit parameter, a `Writes[Iterable[A]]` instance for converting `Result`'s data to JSON.
     * @return A string representing the JSON serialization of the `Result`'s data.
     */
    def json[A](using writer: Writes[Iterable[A]]): FormattingBehavior[A] = (result: Result[A]) =>
      writer.writes(result.data).toString