package org.unibo.scooby
package core.exporter
import core.scraper.Result

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

type ExportingBehavior[A] = Result[A] => Unit

enum ExporterCommands:
  case Export[A](result: Result[A])

case class ExporterOptions[A](exportingFunction: ExportingBehavior[A])

object Exporter:
  import ExporterCommands._

  def stream[A](options: ExporterOptions[A]): Behavior[ExporterCommands] =
    Behaviors.setup : context =>
      Behaviors.receiveMessage :
        case Export(result: Result[A]) =>
          options.exportingFunction(result)
          Behaviors.same

  





