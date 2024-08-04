package org.unibo.scooby
package core.coordinator

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import core.crawler.CrawlerCommand
import core.crawler.CrawlerCommand.CrawlerCoordinatorResponse
import org.unibo.scooby.core.scooby.ScoobyCommand
import org.unibo.scooby.core.scooby.ScoobyCommand.RobotsChecked
import utility.http.URL
import utility.http.URL.*

import scala.annotation.unused

/**
 * Type alias for a function that a Coordinator policy in terms of a page that should be explored and
 * a list of already explored pages.
 */
type Policy = (URL, Set[URL]) => Boolean

enum CoordinatorCommand:

  /**
   * A message that instructs the Coordinator actor to set up the robots.txt file for a given page.
   *
   * @param page
   *   The page to set up the robots.txt file for.
   * @param replyTo
   *   The actor to which reply when Robots.txt has been checked
   */
  case SetupRobots(page: URL, replyTo: ActorRef[ScoobyCommand])

  /**
   * A message that instructs the Coordinator actor to check a list of pages.
   *
   * @param pages
   *   The list of pages to check.
   * @param replyTo
   *   The actor to send the result to.
   */
  case CheckPages(pages: List[URL], replyTo: ActorRef[CrawlerCoordinatorResponse])

  /**
   * A message that instructs the Coordinator actor to set a list of pages as crawled.
   *
   * @param pages
   *   The list of pages to set as crawled.
   */
  case SetCrawledPages(pages: List[URL])

  /**
   * A message that instructs the Coordinator actor to get the list of crawled pages.
   *
   * @param replyTo
   *   The actor to send the result to.
   */
  case GetCrawledPages(replyTo: ActorRef[List[URL]])

/**
 * The Coordinator object contains the definitions of the messages that the Coordinator actor can receive.
 */
object Coordinator:

  /**
   * The behavior of the Coordinator actor when it is first created.
   */
  def apply(maxNumberOfLinks: Int = 2000, policy: Policy = CoordinatorPolicies.defaultPolicy): Behavior[CoordinatorCommand] =
    Behaviors.setup { context =>
      new Coordinator(context, maxNumberOfLinks, policy).idle(Set.empty, Set.empty)
    }

/**
 * The Coordinator actor.
 *
 * @param context
 *   The context in which the actor is running.
*  @param maxNumberOfLinks
 *   Maximum number of links that can be explored by crawlers
 * @param policy
 *  The policy of the [[Coordinator]]
 *
 */
class Coordinator(
                   @unused context: ActorContext[CoordinatorCommand],
                   maxNumberOfLinks: Int,
                   policy: Policy
                 ):

  import CoordinatorCommand.*

  /**
   * The behavior of the Coordinator actor when it is idle.
   *
   * The method uses the Behaviors.receiveMessage function from the Akka Typed API to define the actor's behavior when
   * it receives a message. It matches on the type of the message and executes the corresponding code block.
   *
   * @param crawledUrls
   *   The set of pages that have been crawled. This is used to keep track of which pages have already been crawled, so
   *   that the actor doesn't crawl the same page multiple times.
   * @return
   *   A [[Behavior]] of [[CoordinatorCommand]] that describes how the actor should process the next message it receives.
   */
  def idle(crawledUrls: Set[URL], blackList: Set[String]): Behavior[CoordinatorCommand] =
    Behaviors.receiveMessage {
      case SetupRobots(url, replyTo) =>
        val disallowed = Robots.getDisallowedFromRobots(url)
        replyTo ! RobotsChecked(disallowed.nonEmpty)
        idle(crawledUrls, disallowed)

      case CheckPages(urls, replyTo) if crawledUrls.size >= maxNumberOfLinks =>
        replyTo ! CrawlerCoordinatorResponse(Iterator.empty)
        Behaviors.same

      case CheckPages(urls, replyTo) =>
        val checkResult = urls.filter(_.isAbsolute)
          .filter(page => policy(page, crawledUrls))

        val checkedUrlAndBlackList = checkResult.filter(url => Robots.canVisit(url.toString, blackList))
        replyTo ! CrawlerCoordinatorResponse(checkedUrlAndBlackList.iterator)
        idle(crawledUrls ++ checkResult.toSet, blackList)

      case SetCrawledPages(urls) =>
        val validPages = urls.toSet.filter(_.isAbsolute)
          .diff(crawledUrls)
          .filterNot(el => crawledUrls.map(_.withoutProtocol).contains(el.withoutProtocol))
        idle(crawledUrls ++ validPages, blackList)

      case GetCrawledPages(replyTo) =>
        replyTo ! crawledUrls.toList
        Behaviors.same

      case null => Behaviors.same
    }

/**
 * Contains the different coordinator policies that can be used by
 * the coordinator.
 */
object CoordinatorPolicies:
  /**
   * Execute the default policy on the page
   *
   * @return the [[Policy]] that should be executed
   */
  def defaultPolicy: Policy = (url: URL, alreadyCrawledUrl: Set[URL]) =>
    !alreadyCrawledUrl.map(_.domain).contains(url.domain)