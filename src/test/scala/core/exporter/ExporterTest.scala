package org.unibo.scooby
package core.exporter

import akka.actor.testkit.typed.scaladsl.BehaviorTestKit
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import ExporterCommands.*
import core.scraper.Result

import core.exporter.Exporter.*
import java.nio.file.{Files, Path}
import scala.compiletime.uninitialized


class ExporterTest extends AnyFlatSpec, Matchers, BeforeAndAfterEach:

  var path: Path = uninitialized

  override def beforeEach(): Unit =
    path = Files.createTempDirectory("exporter-tests")
    path.toFile.deleteOnExit()

  override def afterEach(): Unit =
    Files.walk(path)
      .sorted(java.util.Comparator.reverseOrder())
      .forEach(Files.deleteIfExists(_))

  "StreamExporter" should "receive Export message and call exporting function" in:
    val filePath = path.resolve("test.txt")
    val testKit = BehaviorTestKit(stream(ExportingBehaviors.writeOnFile(filePath)))
    testKit.run(Export(Result((1 to 5).toList)))

    Files.exists(filePath) shouldBe true
    Files.readAllLines(filePath).get(0) shouldBe "List(1, 2, 3, 4, 5)"

  "BatchExporter" should "receive Export messages and export only on SignalEnd" in:
    val filePath = path.resolve("test.txt")
    val testKit = BehaviorTestKit(batch(ExportingBehaviors.writeOnFile(filePath))(AggregationBehaviors.default))
    testKit.run(Export(Result((1 to 5).toList)))

    Files.exists(filePath) shouldBe false
    testKit.run(Export(Result((6 to 10).toList)))
    Files.exists(filePath) shouldBe false

    testKit.run(SignalEnd())
    Files.exists(filePath) shouldBe true
    Files.readAllLines(filePath).get(0) shouldBe "List(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)"
