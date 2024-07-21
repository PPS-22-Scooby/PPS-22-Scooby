package org.unibo.scooby
package core.exporter

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors

/**
 * The [[ExporterRouter]] object provides a mechanism to route messages to a sequence of exporter actors.
 *
 * == Overview ==
 * Once the set of [[Exporter]] actors has been defined for transforming results, they should be passed to an
 * [[ExporterRouter]] instance to send [[ExporterCommands]] messages to them.
 *
 * == Example Usage ==
 * {{{
 * val exporterRefs: Seq[ActorRef[ExporterCommands]] = // initialize exporter actor references
 * val routerBehavior: Behavior[ExporterCommands] = ExporterRouter(exporterRefs)
 * val routerActor: ActorRef[ExporterCommands] = context.spawn(routerBehavior, "exporterRouter")
 * }}}
 *
 * @constructor Creates an instance of [[ExporterRouter]].
 */
object ExporterRouter:
  /**
   * Creates a behavior that routes incoming [[ExporterCommands]] messages to all specified exporter actors.
   *
   * @param exporters A sequence of [[ActorRef[ExporterCommands]]] representing the exporter actors to which messages will be routed.
   * @return A [[Behavior[ExporterCommands]] that defines how the router actor will handle incoming messages.
   */
  def apply(exporters: Seq[ActorRef[ExporterCommands]]): Behavior[ExporterCommands] =
    Behaviors.receiveMessage : msg =>
      exporters.foreach(_ ! msg)
      Behaviors.same
