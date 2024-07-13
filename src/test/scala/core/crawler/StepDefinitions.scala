package org.unibo.scooby
package core.crawler

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, BehaviorTestKit}
import io.cucumber.scala.{EN, ScalaDsl}
import core.coordinator.CoordinatorCommand
import utility.http.{Request, URL}

import akka.actor.testkit.typed.CapturedLogEvent
import akka.actor.testkit.typed.scaladsl.FishingOutcomes.fail
import org.scalatest.matchers.should.Matchers.shouldBe
import org.slf4j.event.Level
import utility.http.Clients.SimpleHttpClient

import org.unibo.scooby.core.exporter.ExporterCommands


class StepDefinitions extends ScalaDsl with EN :

  val testKit = ActorTestKit()
  val coordinatorProbe = testKit.createTestProbe[CoordinatorCommand]()
  val exporterProbe = testKit.createTestProbe[ExporterCommands]()
  val behaviorTestKit = BehaviorTestKit(Crawler(coordinatorProbe.ref, exporterProbe.ref, _.content, 
    _.frontier.map(URL(_).getOrElse(URL.empty)), 3))

  var url: URL = URL.empty

  Given("""an URL of an offline website"""):
    () => url = URL("http://localhost:23111").getOrElse(URL.empty)

  Given("""a user Fred that want to crawl a video url"""):
    () =>
      url = URL("https://dl6.webmfiles.org/big-buck-bunny_trailer.webm").getOrElse(URL.empty)

  Given("""a crawler with a Breadth First Search strategy"""):
    () => throw new io.cucumber.scala.PendingException()

  Given("""a crawler that hasn't be started yet"""):
    () => throw new io.cucumber.scala.PendingException()

  Given("""a crawler with a certain exploration strategy"""):
    () => throw new io.cucumber.scala.PendingException()

  Given("""a crawler set up for start exploring the seed url http:\/\/www.adomain.com\/"""):
    () => throw new io.cucumber.scala.PendingException()

  Given("""a crawler setup for explore a website"""):
    () => throw new io.cucumber.scala.PendingException()

  Given("""an user Giovanni that want to set up a custom exploration rule for a website"""):
    () => throw new io.cucumber.scala.PendingException()

  Given("""an user Matteo that want create a custom exploration rule"""):
    () => throw new io.cucumber.scala.PendingException()

  When("""a crawler tries to fetch data from it"""):
    () =>
      behaviorTestKit.run(CrawlerCommand.Crawl(url))

  When("""it will start crawling"""):
    () =>
      behaviorTestKit.run(CrawlerCommand.Crawl(url))

  When("""we assign a new exploration strategy"""):
    () => throw new io.cucumber.scala.PendingException()

  When("""it starts crawling the website reaching http:\/\/www.bdomain.com\/"""):
    () => throw new io.cucumber.scala.PendingException()

  When("""it starts crawling the website"""):
    () => throw new io.cucumber.scala.PendingException()

  When("""he creates a new crawler"""):
    () => throw new io.cucumber.scala.PendingException()


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

  Then("""it will explore only the pages of the internal domain"""):
    () => throw new io.cucumber.scala.PendingException()

  Then("""it will explore the external url"""):
    () => throw new io.cucumber.scala.PendingException()

  Then("""it will start crawl using the last exploration strategy setted"""):
    () => throw new io.cucumber.scala.PendingException()

  Then("""it will visit only the pages listed on robot.txt"""):
    () => throw new io.cucumber.scala.PendingException()

  Then("""it will visit only the pages listed on the whitelist"""):
    () => throw new io.cucumber.scala.PendingException()

  Then("""it will visit only the pages not listed on the blacklist"""):
    () => throw new io.cucumber.scala.PendingException()

  Then("""it will ignore the blacklist"""):
    () => throw new io.cucumber.scala.PendingException()

  Then("""it will start explore the website using the custom rule of Giovanni"""):
    () => throw new io.cucumber.scala.PendingException()

  Then("""it will be able to combine the two rules applying first the bfs one and then the link-weighted one."""):
    () => throw new io.cucumber.scala.PendingException()

  And("""the url will return the Content-Type header video\/webm"""):
    () =>
      val httpClient: SimpleHttpClient = SimpleHttpClient()
      Request.builder.get().at(url).build match
        case Right(request: Request) => request.send(httpClient) match
          case Right(response) =>
            response.headers.get("content-type") shouldBe Some("video/webm")
          case _ => fail("Invalid response")
        case _ => fail("Invalid URL")

  And("""the crawler is not allowed to explore external domains"""):
    () => throw new io.cucumber.scala.PendingException()

  And("""the crawler is allowed to explore external domains"""):
    () => throw new io.cucumber.scala.PendingException()

  And("""the user haven't provided a white or black list"""):
    () => throw new io.cucumber.scala.PendingException()

  And("""the user provided a whitelist"""):
    () => throw new io.cucumber.scala.PendingException()

  And("""the user provided a blacklist"""):
    () => throw new io.cucumber.scala.PendingException()

  And("""it starts crawling a website"""):
    () =>  throw new io.cucumber.scala.PendingException()

  And("""a website with a robot.txt file on the root path"""):
    () => throw new io.cucumber.scala.PendingException()

  And("""parametrized with a white and a black list"""):
    () => throw new io.cucumber.scala.PendingException()

  And("""a BFS exploration rule"""):
    () => throw new io.cucumber.scala.PendingException()

  And("""a link-weighted exploration rule"""):
    () => throw new io.cucumber.scala.PendingException()




