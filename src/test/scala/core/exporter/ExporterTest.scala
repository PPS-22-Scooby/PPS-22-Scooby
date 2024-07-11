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



//  val listCount: ExportingBehavior = (result: Result[?]) =>
//    result.data.map {
//      case (key, value) => s"[$key,$value]"
//    }.mkString("[", ", ", "]")
//
//  val csvContent: ExportingBehavior = (result: Result[?]) =>
//    result.data.map { case (key, value) => s"$key,$value" }.mkString("\n")

//  "Exporter" should "write to file" in:
//    val filePath = path.resolve("test.txt")
//    val testKit = BehaviorTestKit(Exporter(ExporterOptions(_.data.toString, filePath.toString)))
//    testKit.run(Export(Result("test")))
//
//    Files.exists(filePath) shouldBe true
//    Files.readAllLines(filePath).get(0) shouldBe "test"
//
//  it should "log error" in:
//    val filePath = path.resolve("test.txt")
//    val testKit = BehaviorTestKit(Exporter(ExporterOptions(_ => throw new Exception("test"), filePath.toString)))
//    testKit.run(Export(Result("test")))
//
//    testKit.logEntries() shouldBe Seq(
//      CapturedLogEvent(Level.ERROR, f"Error while writing to file: test")
//    )
//
//  it should "append content to file" in:
//    val filePath = path.resolve("test.txt")
//    val testKit = BehaviorTestKit(Exporter(ExporterOptions(_.data.toString, filePath.toString)))
//    testKit.run(Export(Result("test")))
//    testKit.run(Export(Result("test2")))
//
//    val fileContent = Files.readAllLines(filePath)
//    fileContent.size shouldBe 2
//    fileContent should contain allOf("test", "test2")
//
//  it should "write list count to file" in:
//    val filePath = path.resolve("test.txt")
//
//    val testKit = BehaviorTestKit(Exporter(ExporterOptions(listCount, filePath.toString)))
//    testKit.run(Export(Result(Map("test" -> 1, "test2" -> 2))))
//
//    Files.readAllLines(filePath).get(0) shouldBe "[[test,1], [test2,2]]"
//
//  it should "write empty list count to file" in:
//    val filePath = path.resolve("test.txt")
//    val testKit = BehaviorTestKit(Exporter(ExporterOptions(listCount, filePath.toString)))
//    testKit.run(Export(Result(Map.empty)))
//    Files.readAllLines(filePath).get(0) shouldBe "[]"
//
//  it should "write csv content to file" in:
//    val filePath = path.resolve("test.csv")
//    val testKit = BehaviorTestKit(Exporter(ExporterOptions(csvContent, filePath.toString)))
//    testKit.run(Export(Result(Map("test" -> 1, "test2" -> 2))))
//
//    Files.readAllLines(filePath).get(0) shouldBe "test,1"
//    Files.readAllLines(filePath).get(1) shouldBe "test2,2"
//
//  it should "write empty csv content to file" in :
//    val filePath = path.resolve("test.csv")
//    val testKit = BehaviorTestKit(Exporter(ExporterOptions(csvContent, filePath.toString)))
//    testKit.run(Export(Result(Map.empty)))
//
//    Files.readAllLines(filePath).get(0) shouldBe ""







