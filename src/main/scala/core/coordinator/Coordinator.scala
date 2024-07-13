package org.unibo.scooby
package core.coordinator

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import utility.rule.ConditionalRule
import core.crawler.CrawlerCommand
import core.crawler.CrawlerCommand.CrawlerCoordinatorResponse

import utility.http.URL
import utility.http.URL.*


enum CoordinatorCommand:

  /**
   * A message that instructs the Coordinator actor to set up the robots.txt file for a given page.
   *
   * @param page
   *   The page to set up the robots.txt file for.
   */
  case SetupRobots(page: URL)

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
   * A message that contains the result of a CheckPages message.
   *
   * @param result
   *   A iterator of pages and their crawled status.
   */
  case PagesChecked(result: Iterator[String])

/**
 * The Coordinator object contains the definitions of the messages that the Coordinator actor can receive.
 */
object Coordinator:

  /**
   * The behavior of the Coordinator actor when it is first created.
   */
  def apply(maxNumberOfLinks: Int = 2000): Behavior[CoordinatorCommand] =
    import utility.rule.Rule.policy
    Behaviors.setup { context =>
      val defaultUrlPolicy: ConditionalRule[(URL, Set[URL])] = policy:
        (url: URL, crawledPages: Set[URL]) => !crawledPages
          .map(_.domain)
          .contains(url.domain)

      new Coordinator(context, defaultUrlPolicy, maxNumberOfLinks).idle(Set.empty, Set.empty)
    }

/**
 * The Coordinator actor.
 *
 * @param context
 *   The context in which the actor is running.
 */
class Coordinator(
                   context: ActorContext[CoordinatorCommand], 
                   urlPolicy: ConditionalRule[(URL, Set[URL])],
                   maxNumberOfLinks: Int
                 ):

  import CoordinatorCommand.*

  /**
   * The behavior of the Coordinator actor when it is idle.
   *
   * The method uses the Behaviors.receiveMessage function from the Akka Typed API to define the actor's behavior when
   * it receives a message. It matches on the type of the message and executes the corresponding code block.
   *
   * @param crawledPages
   *   The set of pages that have been crawled. This is used to keep track of which pages have already been crawled, so
   *   that the actor doesn't crawl the same page multiple times.
   * @return
   *   A Behavior[Command] that describes how the actor should process the next message it receives.
   */
  def idle(crawledPages: Set[URL], blackList: Set[String]): Behavior[CoordinatorCommand] =
    Behaviors.receiveMessage {
      case SetupRobots(page) =>
        val disallowed = Robots.parseRobotsTxt(Robots.fetchRobotsTxt(page.toString))
        idle(crawledPages, disallowed)

      case CheckPages(pages, replyTo) if pages.size > maxNumberOfLinks =>
        replyTo ! CrawlerCoordinatorResponse(Iterator.empty)
        Behaviors.same
        
      case CheckPages(pages, replyTo) =>
        val checkResult = pages.filter(page => urlPolicy.executeOn(page, crawledPages))
        val checkedUrlAndBlackList = checkResult.filter(url => Robots.canVisit(url.toString, blackList))
        replyTo ! CrawlerCoordinatorResponse(checkedUrlAndBlackList.iterator)
        idle(crawledPages ++ checkResult.toSet, blackList)

      case SetCrawledPages(pages) =>
        val validPages = pages.toSet.filterNot(_ == URL.empty)
          .diff(crawledPages)
          .filterNot(el => crawledPages.map(_.withoutProtocol).contains(el.withoutProtocol))
        idle(crawledPages ++ validPages, blackList)

      case GetCrawledPages(replyTo) =>
        replyTo ! crawledPages.toList
        Behaviors.same

      case _ => Behaviors.same
    }
