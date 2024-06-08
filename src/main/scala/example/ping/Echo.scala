package org.unibo.scooby
package example.ping

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors

object Echo :
  case class Ping(message: String, response: ActorRef[Pong])
  case class Pong(message: String)

  def apply(): Behavior[Ping] = Behaviors.receiveMessage :
    case Ping(m, replyTo) =>
      replyTo ! Pong(m)
      Behaviors.same
  

