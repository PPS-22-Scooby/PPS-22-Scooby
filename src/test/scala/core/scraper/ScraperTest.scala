package org.unibo.scooby
package core.scraper

import akka.actor.DeadLetter
import akka.actor.testkit.typed.scaladsl.{ActorTestKit, TestProbe}
import akka.actor.typed.{ActorRef, ActorSystem}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.matchers.should.Matchers
import utility.document.ScrapeDocument
import utility.http.URL
import core.scraper.ScraperPolicies.{*, given}
import org.scalatest.flatspec.AnyFlatSpec
import core.exporter.ExporterCommands

import scala.compiletime.uninitialized

class ScraperTest extends AnyFlatSpec with Matchers with BeforeAndAfterAll with BeforeAndAfterEach:

  val classSelector: Seq[String] = Seq("testClass1", "testClass2")
  val idSelector: Seq[String] = Seq("testId1", "testId2")
  val idSelectorNoMatch: Seq[String] = Seq("testNoMatch1", "testNoMatch2")
  val cssSelector: Seq[String] = classSelector.map(".".concat)
  val tagSelector: Seq[String] = Seq("li", "p")
  val regEx: String = "testRegex"
  
  val content: String =
    s"""
      |<html lang="en">
      |<head>
      |  <title>Basic HTML Document</title>
      |</head>
      |<body>
      |  <header>
      |    <h1>Welcome to My Website</h1>
      |  </header>
      |  <nav>
      |    <ul>
      |      ${classSelector.zipWithIndex.map { case (cls, index) =>
                s"""<li class="$cls"><a href="#$index">Link $index${if index % 2 == 0 then " " + regEx else ""}</a></li>"""
              }.mkString("\n")}
      |      <li><a href="#about">About</a></li>
      |    </ul>
      |  </nav>
      |  <main>
      |    ${idSelector.zipWithIndex.map { case (id, index) =>
              s"""<section id="$id"><h${index+1}>$id</h2><p>This is $regEx the $id section${if index % 2 == 0 then " " + regEx else ""}.</p></li>"""
            }.mkString("\n")}
      |    <section id="contact">
      |      <h2>Contact</h2>
      |      <p>This is the contact section $regEx.</p>
      |    </section>
      |  </main>
      |  <footer>
      |    <p>&copy; 2024 My Website</p>
      |  </footer>
      |</body>
      |</html>
      |""".stripMargin
  val document: ScrapeDocument = ScrapeDocument(content, URL.empty)

  val testKit: ActorTestKit = ActorTestKit()
  implicit val system: ActorSystem[?] = testKit.system
  val exporterProbe: TestProbe[ExporterCommands] = testKit.createTestProbe[ExporterCommands]()

  var scraperId: ActorRef[ScraperCommands] = uninitialized
  var scraperTag: ActorRef[ScraperCommands] = uninitialized
  var scraperClass: ActorRef[ScraperCommands] = uninitialized
  var scraperCss: ActorRef[ScraperCommands] = uninitialized
  var scraperRegEx: ActorRef[ScraperCommands] = uninitialized
  var scraperMultiPolicies: ActorRef[ScraperCommands] = uninitialized
  var scraperNoMatch: ActorRef[ScraperCommands] = uninitialized

  override def beforeEach(): Unit =
    scraperId = testKit.spawn(Scraper(exporterProbe.ref, ScraperPolicies.scraperPolicy(idSelector, "id")))
    scraperTag = testKit.spawn(Scraper(exporterProbe.ref, ScraperPolicies.scraperPolicy(tagSelector, "tag")))
    scraperClass = testKit.spawn(Scraper(exporterProbe.ref, ScraperPolicies.scraperPolicy(classSelector, "class")))
    scraperCss = testKit.spawn(Scraper(exporterProbe.ref, ScraperPolicies.scraperPolicy(cssSelector, "css")))
    scraperRegEx = testKit.spawn(Scraper(exporterProbe.ref, ScraperPolicies.scraperPolicy(Seq(regEx), "regex")))
    scraperMultiPolicies = testKit.spawn(Scraper(exporterProbe.ref, ScraperPolicies.scraperPolicy(idSelector, "id").concat(ScraperPolicies.scraperPolicy(Seq(regEx), "regex"))))
    scraperNoMatch = testKit.spawn(Scraper(exporterProbe.ref, ScraperPolicies.scraperPolicy(idSelectorNoMatch, "id")))

  override def afterAll(): Unit =
    testKit.shutdownTestKit()

  "Scraper" should "process a document and send the result to exporter" in:
    val expectedById: Result[String] = idSelector.map(document.getElementById).map(_.fold(Result.empty[String])(el => Result.fromData(el.outerHtml)))
      .reduceOption((res1, res2) => res1.aggregate(res2)).getOrElse(Result.empty[String])
    val expectedByTag: Result[String] = tagSelector.flatMap(document.getElementsByTag).map(_.outerHtml).map(elem => Result.fromData(elem))
      .reduceOption((res1, res2) => res1.aggregate(res2)).getOrElse(Result.empty[String])
    val expectedByClass: Result[String] = classSelector.flatMap(document.getElementsByClass).map(_.outerHtml).map(elem => Result.fromData(elem))
      .reduceOption((res1, res2) => res1.aggregate(res2)).getOrElse(Result.empty[String])
    val expectedByCss: Result[String] = cssSelector.flatMap(sel => document.select(sel)).map(_.outerHtml).map(elem => Result.fromData(elem))
      .reduceOption((res1, res2) => res1.aggregate(res2)).getOrElse(Result.empty[String])
    val expectedByRegEx: Result[String] = Result(document.find(regEx))

    scraperId ! ScraperCommands.Scrape(document)
    exporterProbe.expectMessage(ExporterCommands.Export(expectedById))

    scraperTag ! ScraperCommands.Scrape(document)
    exporterProbe.expectMessage(ExporterCommands.Export(expectedByTag))

    scraperClass ! ScraperCommands.Scrape(document)
    exporterProbe.expectMessage(ExporterCommands.Export(expectedByClass))

    scraperCss ! ScraperCommands.Scrape(document)
    exporterProbe.expectMessage(ExporterCommands.Export(expectedByCss))

    scraperRegEx ! ScraperCommands.Scrape(document)
    exporterProbe.expectMessage(ExporterCommands.Export(expectedByRegEx))

  "Scraper" should "support policies concatenation" in:
    scraperMultiPolicies ! ScraperCommands.Scrape(document)
    val firstDocumentContent: String = idSelector.map(document.getElementById).map(_.fold(Result.empty[String])(el => Result.fromData(el.outerHtml)))
      .reduceOption((res1, res2) => res1.aggregate(res2)).getOrElse(Result.empty[String]).data.mkString
    val expected = Result(ScrapeDocument(firstDocumentContent, document.url).find(regEx))
    exporterProbe.expectMessage(ExporterCommands.Export(expected))
    
  "Scraper" should "handle no matches gracefully" in:
    scraperNoMatch ! ScraperCommands.Scrape(document)
    exporterProbe.expectMessage(ExporterCommands.Export(Result.empty[String]))

  "Scraper" should "not be able to process more than one scrape message" in:

    val deadLettersProbe = testKit.createTestProbe[DeadLetter]()
    system.eventStream ! akka.actor.typed.eventstream.EventStream.Subscribe(deadLettersProbe.ref)

    val scraperList = Seq(scraperId, scraperTag, scraperClass, scraperCss, scraperRegEx)

    scraperList.foreach:
      scraper =>
        scraper ! ScraperCommands.Scrape(document)
        deadLettersProbe.expectNoMessage()
        scraper ! ScraperCommands.Scrape(document)
        val deadLetter = deadLettersProbe.expectMessageType[DeadLetter]
        deadLetter.message shouldBe ScraperCommands.Scrape(document)
        deadLetter.recipient.path shouldBe scraper.path
