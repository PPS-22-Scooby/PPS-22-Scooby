package org.unibo.scooby
package example.hello

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}

object HelloWorldMain extends App:

  final case class SayHello(name: String)

  def apply(): Behavior[SayHello] =
    Behaviors.setup : context =>
      val greeter = context.spawn(HelloWorld(), "greeter")

      Behaviors.receiveMessage : message =>
        val replyTo = context.spawn(HelloWorldBot(max = 3), message.name)
        greeter ! HelloWorld.Greet(message.name, replyTo)
        Behaviors.same
      
    

  val system: ActorSystem[HelloWorldMain.SayHello] =
    ActorSystem(HelloWorldMain(), "hello")

  system ! HelloWorldMain.SayHello("World")
  system ! HelloWorldMain.SayHello("Akka")

