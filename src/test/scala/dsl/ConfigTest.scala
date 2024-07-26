package org.unibo.scooby
package dsl

import dsl.util.ScoobyTest
import utility.http.ClientConfiguration

import scala.concurrent.duration.DurationInt

class ConfigTest extends ScoobyTest:

  "Network section of the DSL" should "set the Client configuration parameters correctly" in:
    mockedScooby:
      config:
        network:
          Timeout is 5.seconds
          MaxRequests is 200
    .assertSame(ClientConfiguration(5.seconds, 200, Map.empty))

  "Options section of the DSL" should "set the Options configuration parameters correctly" in:
    mockedScooby:
      config:
        options:
          MaxDepth is 25
          MaxLinks is 4
    .assertSame(25, 4)

  "Headers section of the DSL" should "set the Headers configuration parameters correctly" in:
    mockedScooby:
      config:
        network:
          headers:
            "Agent" to "Me"
            "Content" to "Test"
            "AnotherHeader" to "BeSure"
    .assertSame(ClientConfiguration(headers = Map(
      "Agent" -> "Me",
      "Content" -> "Test",
      "AnotherHeader" -> "BeSure")))

  "Not providing any configuration" should "result in the default configuration" in:
    mockedScooby:
      scrape:
        elements
    .assertSame(ClientConfiguration.default)
    .assertSame(0, 100)

