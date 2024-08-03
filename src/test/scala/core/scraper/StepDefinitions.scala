package org.unibo.scooby
package core.scraper

import io.cucumber.scala.{EN, ScalaDsl}
import utility.document.ScrapeDocument
import utility.http.URL

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, TestProbe}
import play.api.libs.json.*
import akka.actor.typed.{ActorRef, ActorSystem}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import core.exporter.ExporterCommands

import scala.compiletime.uninitialized

class StepDefinitions extends AnyFlatSpec, Matchers, BeforeAndAfterAll, ScalaDsl, EN:

  private var scraperActor: ActorRef[ScraperCommands] = uninitialized
  private var docContent: String = uninitialized
  private var docUrl: URL = uninitialized
  private var scrapeDocument: ScrapeDocument = uninitialized
  private var result: Result[String] = uninitialized

  val testKit: ActorTestKit = ActorTestKit()
  implicit val system: ActorSystem[Nothing] = testKit.system
  val exporterProbe: TestProbe[ExporterCommands] = testKit.createTestProbe[ExporterCommands]()

  override def afterAll(): Unit =
    testKit.shutdownTestKit()

  Given("""I have a scraper with a proper configuration""") : () =>
    val selectors: Seq[String] = Seq("li", "p")

    scraperActor = testKit.spawn(Scraper(exporterProbe.ref, ScraperPolicies.scraperPolicy(selectors, "tag")))
  
  Given("""^I have a scraper with (.*) filtering strategy and (.*) selectors$"""): (by: String, sel: String) =>
    val res = Json.parse(sel).validate[Seq[String]]
    res match
      case JsSuccess(selectors, _) =>
        scraperActor = testKit.spawn(Scraper(exporterProbe.ref, ScraperPolicies.scraperPolicy(selectors, by)))
      case JsError(errors) =>
        println(errors)
  
  And("""I have a document to apply rule to""") : () =>
    docContent =
    s"""
       |<html lang="en">
       |<head>
       |  <title>Basic HTML Document</title>
       |</head>
       |<body>
       |  <nav>
       |    <ul>
       |      <li>About</li>
       |    </ul>
       |  </nav>
       |  <main>
       |    <section id="contact">
       |      <h2>Contact</h2>
       |      <p>This is the contact section.</p>
       |    </section>
       |  </main>
       |  <footer>
       |    <p>2024 My Website</p>
       |  </footer>
       |</body>
       |</html>
       |""".stripMargin
    docUrl = URL.empty
    scrapeDocument = ScrapeDocument(docContent, docUrl)

    result = Result(Seq("<li>About</li>", "<p>This is the contact section.</p>", "<p>2024 My Website</p>"))

  And("""I have a document with no matching""") : () =>
    docContent =
    s"""
       |<html lang="en">
       |<head>
       |  <title>Basic HTML Document</title>
       |</head>
       |</html>
       |""".stripMargin
    docUrl = URL.empty
    scrapeDocument = ScrapeDocument(docContent, docUrl)
    result = Result.empty[String]
  
  And("""^I have the following document as string$""") : (doc: String) =>
    docContent = doc
    docUrl = URL.empty

    scrapeDocument = new ScrapeDocument(docContent, docUrl)
    
  When("""The scraper applies the rule""") : () =>
    scraperActor ! ScraperCommands.Scrape(scrapeDocument)

  Then("""It should send the result""") : () =>
    exporterProbe.expectMessage(ExporterCommands.Export(result))
  
  Then("""It should send an empty result""") : () =>
    exporterProbe.expectMessage(ExporterCommands.Export(result))
  
  Then("""^The scraper should obtain (.*) as result$""") : (sel: String) =>
    val res = Json.parse(sel).validate[Seq[String]]
    res match
      case JsSuccess(expectedResult, _) =>
        exporterProbe.expectMessage(ExporterCommands.Export(Result(expectedResult)))
      case JsError(errors) =>
        println(errors)
