package org.unibo.scooby
package example.ping

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.wordspec.AnyWordSpecLike

class PingTest extends ScalaTestWithActorTestKit with AnyWordSpecLike:

  "Something" must {
    "behave correctly" in {
      val pinger = testKit.spawn(Echo(), "ping")
      val probe = testKit.createTestProbe[Echo.Pong]()
      pinger ! Echo.Ping("hello", probe.ref)
      probe.expectMessage(Echo.Pong("hello"))
    }
  }
