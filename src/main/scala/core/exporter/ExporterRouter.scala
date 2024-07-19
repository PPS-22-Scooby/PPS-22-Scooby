package org.unibo.scooby
package core.exporter

import akka.actor.typed.ActorRef
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{Behaviors, Routers}

object ExporterRouter:
  def apply(exporters: Set[ActorRef[ExporterCommands]], routingKey: String, routerName: String) = Behaviors.setup[Unit]:
    context =>
      val serviceKey: ServiceKey[ExporterCommands.Export[?]] = ServiceKey(routingKey)

      exporters foreach(context.system.receptionist ! Receptionist.Register(serviceKey, _))

      val group = Routers.group(serviceKey)
      val router = context.spawn(group, routerName)

    Behaviors.empty


