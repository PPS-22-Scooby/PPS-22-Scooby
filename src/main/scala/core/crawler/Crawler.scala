package org.unibo.scooby
package core.crawler

import core.coordinator.CoordinatorCommand
import core.crawler.Crawler.getCrawlerName
import core.exporter.ExporterCommands
import core.scraper.{Scraper, ScraperCommands}
import core.scraper.ScraperPolicies.ScraperPolicy
import utility.document.{CrawlDocument, Document, ScrapeDocument}
import utility.http.Clients.SimpleHttpClient
import utility.http.api.Calls.GET
import utility.http.{Configuration, HttpError, HttpErrorType, URL}

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer}
import akka.actor.typed.{ActorRef, Behavior, Terminated}
import org.unibo.scooby.utility.http.Configuration.ClientConfiguration

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

  case ChildTerminated()

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
  def apply[D <: Document, T](
                               coordinator: ActorRef[CoordinatorCommand],
                               exporter: ActorRef[ExporterCommands],
                               scrapeRule: ScraperPolicy[D, T],
                               explorationPolicy: ExplorationPolicy,
                               maxDepth: Int,
                               networkConfiguration: ClientConfiguration = Configuration.default
                             ): Behavior[CrawlerCommand] =
    Behaviors.withStash(50): buffer =>
      Behaviors.setup:
        context => new Crawler[D, T](context, coordinator, exporter, scrapeRule, explorationPolicy,
          maxDepth, networkConfiguration, buffer).idle()

  def getCrawlerName(url: URL): String =
    "[^a-zA-Z0-9\\-_.*$+:@&=,!~';]".r.replaceAllIn(url.withoutProtocol.filter(_ <= 0x7f), ".")


type ExplorationPolicy = CrawlDocument => Iterable[URL]
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
class Crawler[D <: Document, T](context: ActorContext[CrawlerCommand],
                                coordinator: ActorRef[CoordinatorCommand],
                                exporter: ActorRef[ExporterCommands],
                                scrapeRule: ScraperPolicy[D, T],
                                explorationPolicy: ExplorationPolicy,
                                maxDepth: Int,
                                networkConfiguration: ClientConfiguration,
                                buffer: StashBuffer[CrawlerCommand]
                               ):
  import CrawlerCommand.*

  given httpClient: SimpleHttpClient = SimpleHttpClient(networkConfiguration)

  /**
   * The behavior of the Crawler actor.
   *
   * @return
   *   the behavior of the Crawler actor
   */
  def idle(): Behavior[CrawlerCommand] =

    def crawl(url: URL): Behavior[CrawlerCommand] =

      def handleError(e: HttpError): Behavior[CrawlerCommand] =
        e match
          case HttpError(_, HttpErrorType.NETWORK) | HttpError(_, HttpErrorType.GENERIC) =>
            context.log.error(s"Error while crawling $url: ${e.message}")
          case HttpError(_, HttpErrorType.DESERIALIZING) =>
            context.log.error(s"$url does not have a text content type")

        Behaviors.stopped

      def scrape(document: CrawlDocument): Unit =
        val scraper = context.spawn(Scraper(exporter, scrapeRule), s"scraper-${getCrawlerName(url)}")
        context.watchWith(scraper, ChildTerminated())
        scraper ! ScraperCommands.Scrape(ScrapeDocument(document.content, document.url))

      def checkPages(document: CrawlDocument): Unit =
        this.coordinator ! CoordinatorCommand.CheckPages(explorationPolicy(document).map(_.toString).toList, context.self)

      val documentEither: Either[HttpError, CrawlDocument] = GET(url)
      documentEither match
        case Left(e) => handleError(e)
        case Right(document) =>
          scrape(document)
          if maxDepth > 0 then
            checkPages(document)
            Behaviors.same
          else
            context.log.info(s"${context.self.path.name} has reached max depth! Terminating...")
            Behaviors.stopped

    def visitChildren(links: Iterator[String]): Behavior[CrawlerCommand] =
      val linkList = links.toList
      for
        returnedUrl <- linkList
        url <- URL(returnedUrl).toOption
      do
        context.log.info(s"Crawling: ${url.toString}")
        val child = context.spawn(Crawler(coordinator, exporter, scrapeRule, explorationPolicy, maxDepth-1),
          s"crawler-${getCrawlerName(url)}")
        context.watchWith(child, ChildTerminated())
        child ! Crawl(url)

      buffer.unstashAll(waitingForChildren(linkList.size + 1))

    Behaviors.receiveMessage:
      case Crawl(url) => crawl(url)
      case CrawlerCoordinatorResponse(links) => visitChildren(links)
      case x: ChildTerminated =>
        buffer.stash(x)
        Behaviors.same

  private def waitingForChildren(alive: Int): Behavior[CrawlerCommand] =
    context.log.info(s"${context.self.path.name} -> Children alive: $alive")
    if alive == 0 then
      context.log.info(s"Crawler ${context.self.path.name} has no child -> Terminating")
      Behaviors.stopped
    else
      Behaviors.receiveMessage:
        case ChildTerminated() =>
          context.log.info(s"Child terminated")
          waitingForChildren(alive - 1)
        case _ => Behaviors.same







