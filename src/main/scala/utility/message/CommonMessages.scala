package org.unibo.scooby
package utility.message

import akka.actor.Actor.Receive
import akka.actor.{Actor, Props, Stash}

/**
 * Common messages between actors.
 */
object CommonMessages:

  /**
   * Enum representing all common messages.
   */
  enum CommonMessages:
    case Pause
    case Resume

  /**
   * Regulates pause [[Behavior]] of the system actors.
   * @tparam T the [[Actor]] to which apply the paused behavior.
   * @return a new [[Receive]] function.
   */
  def onPaused[T <: Actor & Stash]: (T, Receive) => Receive = (actor, activeReceive) =>
    case CommonMessages.Resume =>
      actor.unstashAll()
      actor.context.become(activeReceive)
    case _ =>
      actor.stash()
