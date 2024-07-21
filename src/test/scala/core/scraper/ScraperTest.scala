package org.unibo.scooby
package core.scraper

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, TestProbe}
import akka.actor.typed.{ActorRef, ActorSystem}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import utility.document.ScrapeDocument
import utility.http.URL
import core.scraper.ScraperPolicies.{given, _}
import org.scalatest.flatspec.AnyFlatSpec

import core.exporter.ExporterCommands

class ScraperTest extends AnyFlatSpec with Matchers with BeforeAndAfterAll:

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
                s"""<li class="$cls"><a href="#${index}">Link $index${if index % 2 == 0 then " " + regEx else ""}</a></li>"""
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
  implicit val system: ActorSystem[Nothing] = testKit.system
  val exporterProbe: TestProbe[ExporterCommands] = testKit.createTestProbe[ExporterCommands]()

  val scraperId: ActorRef[ScraperCommands] = testKit.spawn(Scraper(exporterProbe.ref, ScraperPolicies.scraperRule(idSelector, "id")))
  val scraperTag: ActorRef[ScraperCommands] = testKit.spawn(Scraper(exporterProbe.ref, ScraperPolicies.scraperRule(tagSelector, "tag")))
  val scraperClass: ActorRef[ScraperCommands] = testKit.spawn(Scraper(exporterProbe.ref, ScraperPolicies.scraperRule(classSelector, "class")))
  val scraperCss: ActorRef[ScraperCommands] = testKit.spawn(Scraper(exporterProbe.ref, ScraperPolicies.scraperRule(cssSelector, "css")))
  val scraperRegEx: ActorRef[ScraperCommands] = testKit.spawn(Scraper(exporterProbe.ref, ScraperPolicies.scraperRule(Seq(regEx), "regex")))
  val scraperMultiPolicies: ActorRef[ScraperCommands] = testKit.spawn(Scraper(exporterProbe.ref, ScraperPolicies.scraperRule(idSelector, "id").concat(ScraperPolicies.scraperRule(Seq(regEx), "regex"))))
  val scraperNoMatch: ActorRef[ScraperCommands] = testKit.spawn(Scraper(exporterProbe.ref, ScraperPolicies.scraperRule(idSelectorNoMatch, "id")))

  override def beforeAll(): Unit =
    // system = ActorSystem("ScraperTestSystem")

  "Scraper" should "process Messages.Scrape message correctly" in:

      scraperId ! ScraperCommands.Scrape(document)
      scraperTag ! ScraperCommands.Scrape(document)
      scraperClass ! ScraperCommands.Scrape(document)
      scraperCss ! ScraperCommands.Scrape(document)
      scraperRegEx ! ScraperCommands.Scrape(document)

      scraperId ! ScraperCommands.Scrape(document)
      scraperTag ! ScraperCommands.Scrape(document)
      scraperClass ! ScraperCommands.Scrape(document)
      scraperCss ! ScraperCommands.Scrape(document)
      scraperRegEx ! ScraperCommands.Scrape(document)

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

  override def afterAll(): Unit =
    testKit.shutdownTestKit()
