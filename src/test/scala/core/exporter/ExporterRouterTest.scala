package org.unibo.scooby
package core.exporter

import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import akka.actor.testkit.typed.scaladsl.{ActorTestKit, BehaviorTestKit}
import core.scraper.Result

import akka.actor.typed.ActorRef
import org.unibo.scooby.core.scooby.{Configuration, ScoobyActor, ScoobyCommand}

import scala.language.postfixOps

class ExporterRouterTest extends AnyFlatSpec, Matchers, BeforeAndAfterAll:

  val testKit: ActorTestKit = ActorTestKit()
  val probe1 = testKit.createTestProbe[ExporterCommands]()
  val probe2 = testKit.createTestProbe[ExporterCommands]()
  val probe3 = testKit.createTestProbe[ExporterCommands]()
  val exporters = Seq(probe1.ref, probe2.ref, probe3.ref)
  val mockScooby: ActorRef[ScoobyCommand] = BehaviorTestKit(ScoobyActor(Configuration.empty)).ref

  "ExporterRouterTest" should "broadcast messages to other exporters" in:

    val router = testKit.spawn(ExporterRouter(exporters))
    val command = ExporterCommands.Export(Result((1 to 5).toList))

    router ! command

    probe1.expectMessage(command)
    probe2.expectMessage(command)
    probe3.expectMessage(command)


  it should "broadcast different types of messages" in :
    val router = testKit.spawn(ExporterRouter(exporters))

    val command1 = ExporterCommands.Export(Result((1 to 5).toList))
    val command2 = ExporterCommands.SignalEnd(mockScooby)

    router ! command1
    router ! command2

    probe1.expectMessage(command1)
    probe2.expectMessage(command1)
    probe3.expectMessage(command1)

    probe1.expectMessage(command2)
    probe2.expectMessage(command2)
    probe3.expectMessage(command2)


  it should "handle exporter failure without crashing" in :
    val probe1 = testKit.createTestProbe[ExporterCommands]()
    val probe2 = testKit.createTestProbe[ExporterCommands]()
    val probe3 = testKit.createTestProbe[ExporterCommands]()

    val exporters = Seq(probe1.ref, probe2.ref, probe3.ref)
    val router = testKit.spawn(ExporterRouter(exporters))

    val command = ExporterCommands.Export(Result((1 to 5).toList))

    probe2.stop()

    router ! command

    probe1.expectMessage(command)
    probe3.expectMessage(command)
    