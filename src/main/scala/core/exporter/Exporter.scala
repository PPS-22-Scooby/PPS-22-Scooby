package org.unibo.scooby
package core.exporter
import core.scraper.Result

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, StandardOpenOption}
import scala.util.Try

type ExportingBehavior[A] = Result[A] => Unit
type AggregationBehavior[A] = (Result[A], Result[A]) => Result[A]
type FormattingBehavior[A] = Result[A] => String

enum ExporterCommands:
  case Export[A](result: Result[A])       // Receive results (so you can aggregate or stream them)
  case SignalEnd()                        // Signal the end of the computation so the exporter can write content

object Exporter:
  import ExporterCommands._

  def stream[A](exportingFunction: ExportingBehavior[A]): Behavior[ExporterCommands] =
    Behaviors.setup : context =>
      Behaviors.receiveMessage :
        case Export(result: Result[A]) =>
          exportingFunction(result)
          Behaviors.same
        case SignalEnd() =>
          context.log.error("Stream Exporter can only accept stream results")
          Behaviors.same

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

  def batch[A](exportingFunction: ExportingBehavior[A])
              (aggregation: AggregationBehavior[A]): Behavior[ExporterCommands] =
    fold(Result.empty[A])(exportingFunction)(aggregation)




  object ExportingBehaviors:

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

    def writeOnConsole[A](format: FormattingBehavior[A] = Formats.string) : ExportingBehavior[A] =
      (result: Result[A]) => println(format(result))

  object AggregationBehaviors:
    def default[A]: AggregationBehavior[A] = (res1: Result[A], res2: Result[A]) => res1.aggregate(res2)

  object Formats:
    def string[A]: FormattingBehavior[A] = (result: Result[A]) => result.data.toString() + System.lineSeparator()