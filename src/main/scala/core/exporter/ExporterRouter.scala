package org.unibo.scooby
package core.exporter

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors

object ExporterRouter:
  def apply(exporters: Seq[ActorRef[ExporterCommands]]): Behavior[ExporterCommands] =
    Behaviors.receiveMessage : msg =>
      exporters.foreach(_ ! msg)
      Behaviors.same
