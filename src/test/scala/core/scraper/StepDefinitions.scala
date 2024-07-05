package org.unibo.scooby
package core.scraper

import io.cucumber.scala.{EN, ScalaDsl}
import utility.document.Document

import org.junit.Assert.*
import utility.http.URL

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class StepDefinitions extends TestKit(ActorSystem("TestSystem"))
  with ImplicitSender with AnyWordSpecLike with Matchers with BeforeAndAfterAll
  with ScalaDsl with EN:

  private var scraper = system.actorOf(Props.empty)
  private var docContent: String = _
  private var docUrl: String = _
  private var document: Document = _
  private var result: String = _

  override def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  Given("""^I have a Scraper with a proper configuration$""") : () =>
    // this.scraper = new ScraperActor
    // Create the ActorSystem
    val system = ActorSystem("ScraperSystem")

    // Create an actor instance
    val scraperActor = system.actorOf(ScraperActor.props(sampleScrapeRule), "scraperActor")
    print("Not implemented yet")

  Given("""^I have an empty document$""") : () =>
    docContent = ""
    docUrl = ""

  Given("""^I have the following document$""") : (doc: String) =>
    docContent = doc
    docUrl = ""

  When("""^I try to load it$""") : () =>
    document = Document(docContent, URL(docUrl).getOrElse(URL.empty))

  Then("""^it should return the following as structured document$""") : (expectedOutput: String) =>
    assertEquals(expectedOutput, document.content)

  Then("""^It should be (.*)$""") : (expectedOutput: String) =>
    assertEquals(expectedOutput, document.content)

