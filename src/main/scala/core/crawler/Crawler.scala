package org.unibo.scooby
package core.crawler

import core.coordinator.CoordinatorCommand
import core.crawler.Crawler.getCrawlerName
import core.exporter.ExporterCommands
import core.scraper.ScraperPolicies.ScraperPolicy
import core.scraper.{Scraper, ScraperCommands}

import utility.document.{CrawlDocument, Document, ScrapeDocument}
import utility.http.Clients.SimpleHttpClient
import utility.http.Configuration.ClientConfiguration
import utility.http.api.Calls.GET
import utility.http.{Configuration, HttpError, HttpErrorType, URL}
import utility.http.URL.*

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer}
import akka.actor.typed.{ActorRef, Behavior}

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
  case CrawlerCoordinatorResponse(result: Iterator[URL])

  /**
   * Command for when a child of this Crawler gets terminated
   */
  case ChildTerminated()

object Crawler:
  /**
   * Creates a new Crawler actor.
   *
   * @param coordinator
   *   the ActorRef of the coordinator to communicate with
   * @param exporter
   *   the ActorRef of the exporter to communicate with
   * @param scrapeRule scraping rule for the [[Scraper]]s spawned by this Crawler
   * @param explorationPolicy policy that specifies what links to explore inside the [[CrawlDocument]]
   * @param maxDepth max recursion depth for crawlers (maximum depth of the tree of crawlers)
   * @param networkConfiguration network configuration for the HTTP client
   * @tparam T type of [[Result]]s that get exported
   * @return
   *   the behavior of the Crawler actor
   */
  def apply[T](
                               coordinator: ActorRef[CoordinatorCommand],
                               exporter: ActorRef[ExporterCommands],
                               scrapeRule: ScraperPolicy[T],
                               explorationPolicy: ExplorationPolicy,
                               maxDepth: Int,
                               networkConfiguration: ClientConfiguration = Configuration.default
                             ): Behavior[CrawlerCommand] =
    Behaviors.withStash(50): buffer =>
      Behaviors.setup:
        context => new Crawler[T](context, coordinator, exporter, scrapeRule, explorationPolicy,
          maxDepth, networkConfiguration, buffer).idle()

  /**
   * Obtains a simple identifier for a crawler given its URL
   * @param url url of the given Crawler
   * @return a simple name of the Crawler
   */
  def getCrawlerName(url: URL): String =
    "[^a-zA-Z0-9\\-_.*$+:@&=,!~';]".r.replaceAllIn(url.withoutProtocol.filter(_ <= 0x7f), ".")


/**
 * Type that specifies a function that, given a [[CrawlDocument]], returns the links that need to be explored inside it
 */
type ExplorationPolicy = CrawlDocument => Iterable[URL]
/**
 * Class representing a Crawler actor.
 *
 * @param coordinator
 *   the ActorRef of the coordinator to communicate with
 * @param exporter
 *   the ActorRef of the exporter to communicate with
 * @param scrapeRule scraping rule for the [[Scraper]]s spawned by this Crawler
 * @param explorationPolicy policy that specifies what links to explore inside the [[CrawlDocument]]
 * @param maxDepth max recursion depth for crawlers (maximum depth of the tree of crawlers)
 * @param networkConfiguration network configuration for the HTTP client
 * @tparam T type of [[Result]]s that get exported
 */
class Crawler[T](context: ActorContext[CrawlerCommand],
                                coordinator: ActorRef[CoordinatorCommand],
                                exporter: ActorRef[ExporterCommands],
                                scrapeRule: ScraperPolicy[T],
                                explorationPolicy: ExplorationPolicy,
                                maxDepth: Int,
                                networkConfiguration: ClientConfiguration,
                                buffer: StashBuffer[CrawlerCommand]
                               ):
  import CrawlerCommand.*

  /**
   * Client used for HTTP requests
   * @return the client used
   */
  given httpClient: SimpleHttpClient = SimpleHttpClient(networkConfiguration)

  /**
   * The behavior of the Crawler actor.
   *
   * @return
   *   the behavior of the Crawler actor
   */
  def idle(): Behavior[CrawlerCommand] =
    /**
     * Sub-behavior of the crawler. It fetches the URL and starts crawling.
     * @param url seed URL
     * @return the crawling behavior
     */
    def crawl(url: URL): Behavior[CrawlerCommand] =

      /**
       * Internal function used to handle HTTP errors
       * @param e error to handle
       * @return the behavior in case an error occurred
       */
      def handleError(e: HttpError): Behavior[CrawlerCommand] =
        e match
          case HttpError(_, HttpErrorType.NETWORK) | HttpError(_, HttpErrorType.GENERIC) =>
            context.log.error(s"Error while crawling $url: ${e.message}")
          case HttpError(_, HttpErrorType.DESERIALIZING) =>
            context.log.error(s"$url does not have a text content type")

        Behaviors.stopped

      /**
       * Internal function to handle the scraping (Scraper spawning and watching)
       * @param document document obtained by fetching the URL
       */
      def scrape(document: CrawlDocument): Unit =
        val scraper = context.spawn(Scraper(exporter, scrapeRule), s"scraper-${getCrawlerName(url)}")
        context.watchWith(scraper, ChildTerminated())
        scraper ! ScraperCommands.Scrape(ScrapeDocument(document.content, document.url))

      /**
       * Internal function to handle the communication with the Coordinator to check the valid links to visit.
       * @param document document obtained by fetching the URL
       */
      def checkPages(document: CrawlDocument): Unit =
        context.log.info(document.frontier.toString())
        this.coordinator ! CoordinatorCommand.CheckPages(explorationPolicy(document).toList, context.self)

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

    /**
     * Sub-behavior for the Crawler that recursively spawns the children Crawlers.
     * @param links valid links that get visited. One [[Crawler]] is spawned for each.
     * @return the behavior
     */
    def visitChildren(links: Iterator[URL]): Behavior[CrawlerCommand] =
      val linkList = links.toList
      for
        returnedUrl <- linkList
        url <- Some(returnedUrl)
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

  /**
   * Last behavior for the Crawler. During this phase, the Crawler waits for all its children (crawlers and scraper) to
   * have finished its job. If all the children have terminated, it ends its execution.
   * @param alive number of children to wait
   * @return the waiting behavior
   */
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


object ExplorationPolicies:
  /**
   * Policy that extracts all the links from the document
   * @return all the links in the document
   */
  def allLinks: ExplorationPolicy = _.frontier

  /**
   * Policy that extracts only the links that are in the same domain of the document
   * @return all the links in the document
   */
  def sameDomainLinks: ExplorationPolicy = (document: CrawlDocument) =>
    document.frontier.filter(_.domain == document.url.domain)
      


