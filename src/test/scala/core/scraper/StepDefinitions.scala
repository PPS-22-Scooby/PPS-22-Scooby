package org.unibo.scooby
package core.scraper

import io.cucumber.scala.{EN, ScalaDsl}
import utility.document.{Document, ScrapeDocument, RegExpExplorer}
import utility.http.URL

import play.api.libs.json.*
import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.compiletime.uninitialized

class StepDefinitions extends TestKit(ActorSystem("TestSystem"))
  with ImplicitSender with AnyWordSpecLike with Matchers with BeforeAndAfterAll
  with ScalaDsl with EN:

  private var scraperActor: ActorRef = uninitialized
  private var docContent: String = uninitialized
  private var docUrl: URL = uninitialized
  private var document: Document = uninitialized
  private var scrapeDocument: ScrapeDocument = uninitialized
  private var result: DataResult[String] = uninitialized
  private var probe: TestProbe = uninitialized
  

  override def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  Given("""I have a scraper with a proper configuration""") : () =>
    val system = ActorSystem("ScraperSystem")

    val selectors: Seq[String] = Seq("li", "p")

    scraperActor = system.actorOf(Scraper.props(Scraper.scraperRule(selectors, "tag")), "scraperActor")
  
  Given("""^I have a scraper with (.*) filtering strategy and (.*) selectors$"""): (by: String, sel: String) =>
    val res = Json.parse(sel).validate[Seq[String]]
    res match
      case JsSuccess(selectors: Seq[String], _) =>
        by match
          case "regex" =>
            scraperActor = system.actorOf(Scraper.props(Scraper.regexSelectorsRule(selectors)), "scraperActor")
          case _ =>
            scraperActor = system.actorOf(Scraper.props(Scraper.scraperRule(selectors, by)), "scraperActor")
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

    scrapeDocument = new ScrapeDocument(docContent, docUrl) with RegExpExplorer
    
  When("""The scraper applies the rule""") : () =>
    probe = TestProbe()

    scraperActor.tell(Scraper.ScraperCommands.Scrape(scrapeDocument), probe.ref)

  Then("""It should send the result""") : () =>
    probe.expectMsg(Scraper.ScraperCommands.SendPartialResult(result))
  
  Then("""It should send an empty result""") : () =>
    probe.expectMsg(Scraper.ScraperCommands.SendPartialResult(result))
  
  Then("""^The scraper should obtain (.*) as result$""") : (sel: String) =>
    val res = Json.parse(sel).validate[Seq[String]]
    res match
      case JsSuccess(expectedResult: Seq[String], _) =>
        probe.expectMsg(Scraper.ScraperCommands.SendPartialResult(Result(expectedResult)))
      case JsError(errors) =>
        println(errors)
