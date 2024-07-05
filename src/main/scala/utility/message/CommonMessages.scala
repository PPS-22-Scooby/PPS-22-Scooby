package org.unibo.scooby
package utility.message

import akka.actor.Actor.Receive
import akka.actor.{Actor, Props, Stash}

object CommonMessages:

  enum CommonMessages:
    case Pause
    case Resume

  def onPaused[T <: Actor with Stash]: (T, Receive) => Receive = (actor, activeReceive) =>
    case CommonMessages.Resume =>
      actor.unstashAll()
      actor.context.become(activeReceive)
    case _ =>
      actor.stash()
