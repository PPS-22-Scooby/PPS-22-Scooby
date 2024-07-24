package org.unibo.scooby
package core.exporter

import akka.actor.testkit.typed.scaladsl.BehaviorTestKit
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import ExporterCommands.*
import core.scraper.Result
import core.exporter.Exporter.*

import utility.document.ScrapeDocument
import utility.document.html.HTMLElement
import utility.http.URL

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

  "BatchExporter" should "receive Export messages and export only on SignalEnd" in :
    val filePath = path.resolve("test.txt")
    val testKit = BehaviorTestKit(batch(ExportingBehaviors.writeOnFile(filePath))(AggregationBehaviors.default))
    testKit.run(Export(Result((1 to 5).toList)))

    Files.exists(filePath) shouldBe false
    testKit.run(Export(Result((6 to 10).toList)))
    Files.exists(filePath) shouldBe false

    testKit.run(SignalEnd())
    Files.exists(filePath) shouldBe true
    Files.readAllLines(filePath).get(0) shouldBe "List(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)"

  "BatchExporter" should "receive Export messages and export Json" in :
    import JsonConverter.given
    val filePath = path.resolve("test.txt")
    val document: ScrapeDocument = ScrapeDocument("<div><p>Text 1</p><p>Par 2</p></div><div><p>Text 2</p></div>", URL.empty)
    val htmlElements = document.getElementsByTag("div")
    val testKit = BehaviorTestKit(batch(ExportingBehaviors.writeOnFile[HTMLElement](filePath, Formats.json))(AggregationBehaviors.default))
    testKit.run(Export(Result(htmlElements)))
    Files.exists(filePath) shouldBe false

    testKit.run(SignalEnd())
    Files.exists(filePath) shouldBe true
    Files.readAllLines(filePath).toString should be
    """[{"tag":"div","attributes":{},"text":"Text 1 Par 2","children":[{"tag":"p","attributes":{},"text":"Text 1","children":[]},{"tag":"p","attributes":{},"text":"Par 2","children":[]}]},{"tag":"div","attributes":{},"text":"Text 2","children":[{"tag":"p","attributes":{},"text":"Text 2","children":[]}]}]""".stripMargin