package org.unibo.scooby
package core.exporter
import core.scraper.Result

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

type ExportingBehavior[A] = Result[A] => Unit
type AggregationBehavior[A] = (Result[A], Result[A]) => Result[A]

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

  def batch[A](result: Result[A])
              (exportingFunction: ExportingBehavior[A])
              (aggregation: AggregationBehavior[A]): Behavior[ExporterCommands] =
    Behaviors.setup : context =>
      Behaviors.receiveMessage :
        case Export(newResult: Result[A]) =>
          batch(aggregation(result, newResult))(exportingFunction)(aggregation)
        case SignalEnd() =>
          exportingFunction(result)
          Behaviors.same




