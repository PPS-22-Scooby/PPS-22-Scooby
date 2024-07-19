package org.unibo.scooby
package core.exporter

import akka.actor.typed.ActorRef
import akka.routing.BroadcastGroup

object ExporterRouter:
  def apply(exporters: Seq[ActorRef[ExporterCommands]], routerName: String = "exp-router"): BroadcastGroup =
    BroadcastGroup(exporters.map(_.toString), routerName)
