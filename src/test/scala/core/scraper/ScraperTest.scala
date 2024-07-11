package org.unibo.scooby
package core.scraper

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, BehaviorTestKit, TestProbe}
import akka.actor.typed.{ActorRef, ActorSystem}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import utility.document.ScrapeDocument
import utility.http.URL

import org.scalatest.flatspec.AnyFlatSpec
import core.exporter.ExporterCommands

class ScraperTest extends AnyWordSpecLike, Matchers, BeforeAndAfterAll:

  val classSelector: Seq[String] = Seq("testClass1", "testClass2")
  val idSelector: Seq[String] = Seq("testId1", "testId2")
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
              s"""<section id="$id"><h${index+1}>$id</h2><p>This is the $id section${if index % 2 == 0 then " " + regEx else ""}.</p></li>"""
            }.mkString("\n")}
      |    <section id="contact">
      |      <h2>Contact</h2>
      |      <p>This is the contact section.</p>
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

  val scraperId: ActorRef[ScraperCommands] = testKit.spawn(Scraper(exporterProbe.ref, Scraper.scraperRule(idSelector, "id")))
  val scraperTag: ActorRef[ScraperCommands] = testKit.spawn(Scraper(exporterProbe.ref, Scraper.scraperRule(tagSelector, "tag")))
  val scraperClass: ActorRef[ScraperCommands] = testKit.spawn(Scraper(exporterProbe.ref, Scraper.scraperRule(classSelector, "class")))
  val scraperCss: ActorRef[ScraperCommands] = testKit.spawn(Scraper(exporterProbe.ref, Scraper.scraperRule(cssSelector, "css")))
  val scraperRegEx: ActorRef[ScraperCommands] = testKit.spawn(Scraper(exporterProbe.ref, Scraper.scraperRule(Seq(regEx), "regex")))

  override def beforeAll(): Unit =
    // system = ActorSystem("ScraperTestSystem")

  "Scraper actor" should:
    "process Messages.Scrape message correctly" in:

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

    "process a document and send the result to exporter" in:

      scraperId ! ScraperCommands.Scrape(document)
      scraperTag ! ScraperCommands.Scrape(document)
      scraperClass ! ScraperCommands.Scrape(document)
      scraperCss ! ScraperCommands.Scrape(document)
      scraperRegEx ! ScraperCommands.Scrape(document)

      val expectedById: DataResult[String] = idSelector.map(document.getElementById).map(_.text).map(elem => Result.fromData(elem))
        .reduceOption((res1, res2) => res1.aggregate(res2)).getOrElse(Result.empty[String])
      val expectedByTag: DataResult[String] = tagSelector.flatMap(document.getElementByTag).map(_.text).map(elem => Result.fromData(elem))
        .reduceOption((res1, res2) => res1.aggregate(res2)).getOrElse(Result.empty[String])
      val expectedByClass: DataResult[String] = classSelector.flatMap(document.getElementByClass).map(_.text).map(elem => Result.fromData(elem))
        .reduceOption((res1, res2) => res1.aggregate(res2)).getOrElse(Result.empty[String])
      val expectedByCss: DataResult[String] = cssSelector.flatMap(sel => document.select(sel)).map(_.text).map(elem => Result.fromData(elem))
        .reduceOption((res1, res2) => res1.aggregate(res2)).getOrElse(Result.empty[String])
      val expectedByRegEx: DataResult[String] = Result(document.find(regEx))

      exporterProbe.expectMessage(ExporterCommands.Export(expectedById))
      exporterProbe.expectMessage(ExporterCommands.Export(expectedByTag))
      exporterProbe.expectMessage(ExporterCommands.Export(expectedByClass))
      exporterProbe.expectMessage(ExporterCommands.Export(expectedByCss))
      exporterProbe.expectMessage(ExporterCommands.Export(expectedByRegEx))

  override def afterAll(): Unit =
    testKit.shutdownTestKit()
