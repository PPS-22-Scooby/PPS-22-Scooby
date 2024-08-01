package org.unibo.scooby
package core.crawler

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, BehaviorTestKit, TestProbe}
import io.cucumber.scala.{EN, ScalaDsl}
import core.coordinator.CoordinatorCommand
import utility.http.{ClientConfiguration, Request, URL}
import akka.actor.testkit.typed.CapturedLogEvent
import akka.actor.testkit.typed.scaladsl.FishingOutcomes.fail
import org.scalatest.matchers.should.Matchers.shouldBe
import org.slf4j.event.Level
import utility.http.Clients.SimpleHttpClient
import core.exporter.ExporterCommands
import scala.concurrent.duration.DurationInt


class StepDefinitions extends ScalaDsl with EN :

  val testKit: ActorTestKit = ActorTestKit()
  val coordinatorProbe: TestProbe[CoordinatorCommand] = testKit.createTestProbe[CoordinatorCommand]()
  val exporterProbe: TestProbe[ExporterCommands] = testKit.createTestProbe[ExporterCommands]()
  val behaviorTestKit: BehaviorTestKit[CrawlerCommand] = BehaviorTestKit(Crawler(coordinatorProbe.ref, exporterProbe.ref, _.content,
    ExplorationPolicies.allLinks, 3, 
    networkConfiguration = ClientConfiguration.default.copy(networkTimeout = 3.seconds)))

  var url: URL = URL.empty

  Given("""an URL of an offline website"""):
    () => url = URL("http://localhost:23111")

  Given("""a user Fred that want to crawl a video url"""):
    () =>
      url = URL("https://dl6.webmfiles.org/big-buck-bunny_trailer.webm")
  

  When("""a crawler tries to fetch data from it"""):
    () =>
      behaviorTestKit.run(CrawlerCommand.Crawl(url))

  When("""it will start crawling"""):
    () =>
      behaviorTestKit.run(CrawlerCommand.Crawl(url))
  
  Then("""will notice the user that the url can't be parsed"""):
    () =>
      behaviorTestKit.logEntries() shouldBe Seq(
        CapturedLogEvent(Level.ERROR, f"Error while crawling $url: Exception when sending request: GET $url")
      )

  Then("""will notice the user that the url can't be parsed and continues with other urls"""):
    () =>
      behaviorTestKit.logEntries() shouldBe Seq(
        CapturedLogEvent(Level.ERROR, f"Error while crawling $url: Exception when sending request: GET $url")
      )

  Then("""will notice the user that the url can't be parsed because is not a text file"""):
    () =>
      behaviorTestKit.logEntries() shouldBe Seq(
        CapturedLogEvent(Level.ERROR, s"$url does not have a text content type")
      )
  

  And("""the url will return the Content-Type header video\/webm"""):
    () =>
      val httpClient: SimpleHttpClient = SimpleHttpClient()
      Request.builder.get().at(url).build match
        case Right(request: Request) => request.send(httpClient) match
          case Right(response) =>
            response.headers.get("content-type") shouldBe Some("video/webm")
          case _ => fail("Invalid response")
        case _ => fail("Invalid URL")


