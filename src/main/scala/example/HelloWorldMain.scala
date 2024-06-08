package org.unibo.scooby
package example

import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors

object HelloWorldMain extends App:

  final case class SayHello(name: String)

  def apply(): Behavior[SayHello] =
    Behaviors.setup { context =>
      val greeter = context.spawn(HelloWorld(), "greeter")

      Behaviors.receiveMessage { message =>
        val replyTo = context.spawn(HelloWorldBot(max = 3), message.name)
        greeter ! HelloWorld.Greet(message.name, replyTo)
        Behaviors.same
      }
    }

  val system: ActorSystem[HelloWorldMain.SayHello] =
    ActorSystem(HelloWorldMain(), "hello")

  system ! HelloWorldMain.SayHello("World")
  system ! HelloWorldMain.SayHello("Akka")

