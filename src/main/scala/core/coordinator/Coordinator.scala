package org.unibo.scooby
package core.coordinator

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import utility.rule.ConditionalRule

import core.crawler.CrawlerCommand
import core.crawler.CrawlerCommand.CrawlerCoordinatorResponse

enum CoordinatorCommand:

  /**
   * A message that instructs the Coordinator actor to check a list of pages.
   *
   * @param pages
   *   The list of pages to check.
   * @param replyTo
   *   The actor to send the result to.
   */
  case CheckPages(pages: List[String], replyTo: ActorRef[CrawlerCoordinatorResponse])

  /**
   * A message that instructs the Coordinator actor to set a list of pages as crawled.
   *
   * @param pages
   *   The list of pages to set as crawled.
   */
  case SetCrawledPages(pages: List[String])

  /**
   * A message that instructs the Coordinator actor to get the list of crawled pages.
   *
   * @param replyTo
   *   The actor to send the result to.
   */
  case GetCrawledPages(replyTo: ActorRef[List[String]])

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
   * Normalizes a URL by removing the http:// or https:// prefix.
   *
   * @param url
   *   The URL to normalize.
   * @return
   *   The normalized URL.
   */
  def normalizeURL(url: String): String = url.replaceFirst("^(http://|https://)", "")

  /**
   * Checks if a URL is valid.
   *
   * @param url
   *   The URL to check.
   * @return
   *   True if the URL is valid, false otherwise.
   */
  def isValidURL(url: String): Boolean =
    val regex = "^(http://|https://)[a-zA-Z0-9\\-\\.]+\\.[a-zA-Z]{2,}(\\S*)?$"
    url.matches(regex)

  /**
   * The behavior of the Coordinator actor when it is first created.
   */
  def apply(): Behavior[CoordinatorCommand] =
    import utility.rule.Rule.policy
    Behaviors.setup { context =>
      val defaultUrlPolicy: ConditionalRule[(String, Set[String])] = policy {
        (url: String, crawledPages: Set[String]) =>
          val normalizedPage = normalizeURL(url)
          val isAlreadyCrawled = crawledPages.contains(normalizedPage)
          isValidURL(url) && !isAlreadyCrawled
      }
      new Coordinator(context, defaultUrlPolicy).idle(Set.empty)
    }

/**
 * The Coordinator actor.
 *
 * @param context
 *   The context in which the actor is running.
 */
class Coordinator(context: ActorContext[CoordinatorCommand], urlPolicy: ConditionalRule[(String, Set[String])]):

  import CoordinatorCommand.*
  import Coordinator.*

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
  def idle(crawledPages: Set[String]): Behavior[CoordinatorCommand] =
    Behaviors.receiveMessage {
      case CheckPages(pages, replyTo) =>
        val resultIterator = pages.filter(page => urlPolicy.executeOn(page, crawledPages))
        replyTo ! CrawlerCoordinatorResponse(resultIterator.iterator)
        idle(crawledPages ++ resultIterator.map(normalizeURL).toSet)

      case SetCrawledPages(pages) =>
        val validPages = pages.filter(isValidURL).map(normalizeURL).toSet
        idle(crawledPages ++ validPages)

      case GetCrawledPages(replyTo) =>
        replyTo ! crawledPages.toList
        Behaviors.same

      case _ => Behaviors.same
    }
