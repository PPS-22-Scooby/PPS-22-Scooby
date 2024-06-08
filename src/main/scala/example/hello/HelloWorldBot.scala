package org.unibo.scooby
package example.hello

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

import scala.annotation.tailrec

object HelloWorldBot:

  def apply(max: Int): Behavior[HelloWorld.Greeted] = bot(0, max)

  private def bot(greetingCounter: Int, max: Int): Behavior[HelloWorld.Greeted] =
    Behaviors.receive { (context, message) =>
      val n = greetingCounter + 1
      context.log.info("Greeting {} for {}", n, message.whom)
      if (n == max) {
        Behaviors.stopped
      } else {
        message.from ! HelloWorld.Greet(message.whom, context.self)
        bot(n, max)
      }
    }

