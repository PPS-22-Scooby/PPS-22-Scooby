package org.unibo.scooby
package core.crawler

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import utility.http.{HttpError, HttpErrorType, URL}
import utility.http.api.Calls.GET

import akka.actor.typed.{ActorRef, Behavior}
import utility.http.Clients.SimpleHttpClient
import utility.document.{CrawlDocument, ScrapeDocument}
import core.coordinator.CoordinatorCommand
import core.scraper.Scraper
import core.exporter.ExporterCommands

import scala.language.postfixOps

enum CrawlerCommand:
  /**
   * Command to initiate crawling of the given URL.
   *
   * @param url
   *   the URL to be crawled
   */
  case Crawl(url: URL)

  /**
   * Command to handle the response from the coordinator with the result of checked pages.
   *
   * @param result
   *   a map of URLs to their corresponding statuses
   */
  case CrawlerCoordinatorResponse(result: Iterator[String])

object Crawler:
  /**
   * Creates a new Crawler actor.
   *
   * @param coordinator
   *   the ActorRef of the coordinator to communicate with
   * @param exporter
   *   the ActorRef of the exporter to communicate with
   * @return
   *   the behavior of the Crawler actor
   */
  def apply(coordinator: ActorRef[CoordinatorCommand], exporter: ActorRef[ExporterCommands]): Behavior[CrawlerCommand] =
    Behaviors.setup {
      context => new Crawler(context, coordinator, exporter).idle()
    }

/**
 * Class representing a Crawler actor.
 *
 * @param context
 *   the ActorContext of the Crawler actor
 * @param coordinator
 *   the ActorRef of the coordinator to communicate with
 * @param exporter
 * *   the ActorRef of the exporter to communicate with
 */
class Crawler(context: ActorContext[CrawlerCommand], coordinator: ActorRef[CoordinatorCommand], exporter: ActorRef[ExporterCommands]):
  import CrawlerCommand._

  given httpClient: SimpleHttpClient = SimpleHttpClient()

  /**
   * The behavior of the Crawler actor.
   *
   * @return
   *   the behavior of the Crawler actor
   */
  def idle(): Behavior[CrawlerCommand] = Behaviors.receiveMessage {
    case Crawl(url) =>
      val documentEither: Either[HttpError, CrawlDocument] = GET(url)
      documentEither match
        case Left(e) => e match
          case HttpError(_, HttpErrorType.NETWORK) | HttpError(_, HttpErrorType.GENERIC) =>
            context.log.error(s"Error while crawling $url: ${e.message}")
          case HttpError(_, HttpErrorType.DESERIALIZING) =>
            context.log.error(s"$url does not have a text content type")
        case Right(document) =>
          this.coordinator ! CoordinatorCommand.CheckPages(document.frontier.toList, context.self)
          val scraper = context.spawnAnonymous(Scraper(exporter, Scraper.scraperRule(Seq("body"), "tag")))
          scraper ! Scraper.ScraperCommands.Scrape(ScrapeDocument(document.content, document.url))
      Behaviors.same

    case CrawlerCoordinatorResponse(links) =>
      for
        returnedUrl <- links
        url <- URL(returnedUrl).toOption
      do
        val children = context.spawnAnonymous(Crawler(coordinator, exporter))
        context.log.info(s"Crawl: ${url.toString}")
        children ! Crawl(url)

      Behaviors.same
  }
