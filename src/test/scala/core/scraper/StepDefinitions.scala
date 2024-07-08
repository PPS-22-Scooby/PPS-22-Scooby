package org.unibo.scooby
package core.scraper

import io.cucumber.scala.{EN, ScalaDsl}
import utility.document.Document

import org.junit.Assert.*
import utility.http.URL
import play.api.libs.json._

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class StepDefinitions extends TestKit(ActorSystem("TestSystem"))
  with ImplicitSender with AnyWordSpecLike with Matchers with BeforeAndAfterAll
  with ScalaDsl with EN:

  private var scraperActor: ActorRef = _// ScraperActor[Document, String] = _
  private var docContent: String = _
  private var docUrl: URL = _
  private var document: Document = _
  private var result: Result[String] = _
  private var probe: TestProbe = _
  

  override def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  Given("""I have a Scraper with a proper configuration""") : () =>
    val system = ActorSystem("ScraperSystem")

    val selectors: Seq[String] = Seq("li", "p")

    scraperActor = system.actorOf(ScraperActor.props(ScraperActor.scraperRule(selectors, "tag")), "scraperActor")
  
  Given("""^I have a scraper with (.*) filtering strategy and (.*) selectors$"""): (by: String, sel: String) =>
    val res = Json.parse(sel).validate[Seq[String]]
    res match
      case JsSuccess(selectors: Seq[String], _) =>
        scraperActor = system.actorOf(ScraperActor.props(ScraperActor.scraperRule(selectors, by)), "scraperActor")
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
       |    <p>&copy; 2024 My Website</p>
       |  </footer>
       |</body>
       |</html>
       |""".stripMargin
    docUrl = URL.empty
    document = Document(docContent, docUrl)

    result = Result(Seq("<li>About</li>", "<p>This is the contact section.</p>"))

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
    document = Document(docContent, docUrl)
    result = Result[String].empty
  
  And("""^I have the following document as string (.*)$"""): (doc: String) =>
    docContent = doc
    docUrl = URL.empty

    document = Document(docContent, docUrl)
    
  When("""The scraper applies the rule""") : () =>
    probe = TestProbe()

    scraperActor.tell(ScraperActor.Messages.Scrape(document), probe.ref)

  Then("""It should send the result""") : () =>
    probe.expectMsg(ScraperActor.Messages.SendPartialResult(result))
  
  Then("""It should send an empty result""") : () =>
    probe.expectMsg(ScraperActor.Messages.SendPartialResult(result))
  
  Then("""^The scraper should obtain (.*) as result$""") : (res: String) =>
    val res = Json.parse(sel).validate[Seq[String]]
    res match
      case JsSuccess(expectedResult: Seq[String], _) =>
        probe.expectMsg(ScraperActor.Messages.SendPartialResult(Result(expectedResult)))
      case JsError(errors) =>
        println(errors)
    