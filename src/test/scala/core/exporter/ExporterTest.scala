package org.unibo.scooby
package core.exporter

import akka.actor.testkit.typed.scaladsl.BehaviorTestKit
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import ExporterCommands.*
import core.scraper.ResultImpl

import akka.actor.testkit.typed.CapturedLogEvent
import org.slf4j.event.Level

import java.nio.file.{Files, Path}
import scala.compiletime.uninitialized

class ExporterTest extends AnyFlatSpec, Matchers, BeforeAndAfterAll:

  var path: Path = uninitialized

  override def beforeAll(): Unit = {
    path = Files.createTempDirectory("tempDirPrefix")
  }

  "Exporter" should "write to file" in:
    val filePath = path.resolve("test.txt")
    val testKit = BehaviorTestKit(Exporter(ExporterOptions(_.data.toString, filePath.toString)))
    testKit.run(Export(ResultImpl("test")))

    Files.exists(filePath) shouldBe true
    Files.readAllLines(filePath).get(0) shouldBe "test"

  it should "log error" in:
    val filePath = path.resolve("test.txt")
    val testKit = BehaviorTestKit(Exporter(ExporterOptions(_ => throw new Exception("test"), filePath.toString)))
    testKit.run(Export(ResultImpl("test")))

    testKit.logEntries() shouldBe Seq(
      CapturedLogEvent(Level.ERROR, f"Error while writing to file: test")
    )

  it should "append content to file" in:
    val filePath = path.resolve("test.txt")
    val testKit = BehaviorTestKit(Exporter(ExporterOptions(_.data.toString, filePath.toString)))
    testKit.run(Export(ResultImpl("test")))
    testKit.run(Export(ResultImpl("test2")))

    Files.exists(filePath) shouldBe true

    val fileLines = Files.readAllLines(filePath)
    fileLines.get(0) shouldBe "test"
    fileLines.get(1) shouldBe "test2"







