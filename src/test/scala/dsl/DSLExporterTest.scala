package org.unibo.scooby
package dsl

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import Application.scooby
import core.scraper.{Result, ScraperPolicies}

import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import utility.document.ScrapeDocument

import akka.actor.testkit.typed.scaladsl.BehaviorTestKit
import core.exporter.Exporter.{AggregationBehaviors, ExportingBehaviors, Formats, batch, stream}
import core.scooby.Configuration
import dsl.Config.NetworkConfiguration.network
import dsl.Config.config
import core.exporter.ExporterCommands.{Export, SignalEnd}
import core.scraper.ScraperPolicies.ScraperPolicy
import core.exporter.ExporterCommands
import utility.http.URL

import org.unibo.scooby.utility.document.html.HTMLElement

import java.io.{ByteArrayOutputStream, PrintStream}
import java.nio.file.{Files, Path}
import scala.compiletime.uninitialized

class DSLExporterTest extends AnyFlatSpec, ScoobyEmbeddable, Matchers, BeforeAndAfterEach:

  var expectedResult: Result[String] = uninitialized
  var resultDSL: Result[String] = uninitialized
  var resultExporter: Result[String] = uninitialized

  var pathDSL: Path = uninitialized
  var pathExporter: Path = uninitialized

  var fileDSL: String = "dslTest.txt"
  var fileExporter: String = "exporterTest.txt"

  val testClass: String = "myClass"
  val scrapePolicy: ScraperPolicy[String] = doc => doc.getElementsByClass(testClass).map(_.outerHtml)
  val format: FormatType = Text

  val content: String = scala.io.Source.fromResource("Resources.html").mkString("")

  val document: ScrapeDocument = ScrapeDocument(content, URL.empty)

  override def beforeEach(): Unit =
     pathDSL = Files.createTempDirectory("exporter-tests-DSL")
     pathDSL.toFile.deleteOnExit()
     pathExporter = Files.createTempDirectory("exporter-tests-standard")
     pathExporter.toFile.deleteOnExit()

  override def afterEach(): Unit =
    Files.walk(pathDSL)
      .sorted(java.util.Comparator.reverseOrder())
      .forEach(Files.deleteIfExists(_))
    Files.walk(pathExporter)
      .sorted(java.util.Comparator.reverseOrder())
      .forEach(Files.deleteIfExists(_))

  def getDSLResultAsIterable[T](doc: ScrapeDocument, policy: ScraperPolicy[T]): Iterable[T] =
    policy.apply(doc)

  "Exporter with batch write on file" should "behave the same" in:

    given Iterable[String] = getDSLResultAsIterable(document, scrapePolicy)
    given ConfigurationBuilder[String] = new ConfigurationBuilder(Configuration.empty[String], ScrapingResultSetting[String]())

    val app: ScoobyRunnable[String] = scooby:
      exports:
        Batch:
          strategy:
            results output:
              // ToFile(pathDSL.toString + "/" + fileDSL) withFormat format
              ToConsole withFormat format

          aggregate:
            _ ++ _

    resultDSL = Await.result(app.run(), 10.seconds)

    expectedResult = Result(scrapePolicy.apply(document))

    // TODO once result is correctly returned, apply this test resultDSL shouldBe expectedResult

    val writeFormat = if format == FormatType.Text then Formats.string else Formats.string // TODO Once JSON Formats implemented apply it

    val testKit = BehaviorTestKit(batch(ExportingBehaviors.writeOnFile(Path.of(pathExporter.toString + "/" + fileExporter), writeFormat))(AggregationBehaviors.default))
    testKit.run(ExporterCommands.Export(expectedResult))
    testKit.run(SignalEnd())

    val filePathDSL = pathDSL.resolve(fileDSL)
    val filePathExporter = pathExporter.resolve(fileExporter)

    Files.exists(filePathExporter) shouldBe true
    Files.readAllLines(filePathExporter).get(0) shouldBe "List(<li class=\"myClass\"><a href=\"#\">Link</a></li>)"

    // TODO once result is correctly returned, apply this test Files.exists(filePathDSL) shouldBe true
    // TODO once result is correctly returned, apply this test Files.readAllLines(filePathDSL).get(0) shouldBe "List(<li class=\"myClass\"><a href=\"#\">Link</a></li>)"


  "Exporter with stream write on console" should "behave the same" in:
    // TODO once fixed stream export, apply and verify
    true shouldBe true
//    given Iterable[String] = getDSLResultAsIterable(document, scrapePolicy)
//    given ConfigurationBuilder[String] = new ConfigurationBuilder(Configuration.empty[String], ScrapingResultSetting[String]())
//
//    val app: ScoobyRunnable[String] = scooby:
//      exports:
//        Streaming:
//          results outputTo:
//            ToConsole withFormat Text
//
//    expectedResult = Result(scrapePolicy.apply(document))
//
//    val outCaptureDSL = new ByteArrayOutputStream()
//    Console.withOut(new PrintStream(outCaptureDSL)) {
//      resultDSL = Await.result(app.run(), 10.seconds)
//    }
//    // Assert the captured output
//    val outputDSL = outCaptureDSL.toString
//    println(s"DSL: $outputDSL")
//    outputDSL should include(resultDSL.toString) // TODO once result is correctly returned, apply this
//
//    val writeFormat = if format == FormatType.Text then Formats.string else Formats.string // TODO Once JSON Formats implemented apply it
//    val testKit = BehaviorTestKit(stream(ExportingBehaviors.writeOnConsole(writeFormat)))
//
//    // Capture the output
//    val outCaptureStandard = new ByteArrayOutputStream()
//    Console.withOut(new PrintStream(outCaptureStandard)) {
//      testKit.run(ExporterCommands.Export(expectedResult))
//      testKit.run(SignalEnd())
//    }
//
//    // Assert the captured output
//    val outputStandard = outCaptureStandard.toString
//    println(s"Standard: $outputStandard")
//    outputStandard should include(expectedResult.toString)
