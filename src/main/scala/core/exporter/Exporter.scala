package org.unibo.scooby
package core.exporter
import core.scraper.ResultImpl

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths, StandardOpenOption}
import scala.util.Try

type ExportingBehavior = ResultImpl[?] => String

enum ExporterCommands:
  case Export(result: ResultImpl[?])

case class ExporterOptions(exportingFunction: ExportingBehavior, outputFilePath: String)

object Exporter:
  import ExporterCommands._
  def apply[A, B](options: ExporterOptions): Behavior[ExporterCommands] =
    Behaviors.setup { context =>
      Behaviors.receiveMessage {
        case Export(result) =>
          Try {
            val writer = Files.newBufferedWriter(Paths.get(options.outputFilePath), StandardCharsets.UTF_8,
              StandardOpenOption.CREATE, StandardOpenOption.APPEND)
            val content = options.exportingFunction(result) + System.lineSeparator()
            (writer, content)
          }.toEither match
            case Left(exception) =>
              context.log.error(s"Error while writing to file: ${exception.getMessage}")
            case Right(writer, content) =>
              writer.write(content)
              writer.close()
          Behaviors.same
      }
    }






