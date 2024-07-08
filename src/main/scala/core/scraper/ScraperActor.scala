package org.unibo.scooby
package core.scraper

import utility.document.{Document, RegExpExplorer, ScrapeDocument}
import utility.message.CommonMessages.{CommonMessages, onPaused}

import akka.actor.{Actor, ActorSystem, Props, Stash}

/**
 * A type representing a function that extract an [[Iterable]] used to build [[Result]] from a [[Document]]
 * @tparam D a type which is a subtype of [[Document]] type.
 * @tparam T a generic type which represents the expected [[Result]] type.
 */
type IterableFromDoc[D <: Document, T] = D => Iterable[T]

/**
 * Class representing Scraper actor.
 * @param scrapeRule the scraping rule the actor uses.
 * @tparam D the type representing the [[Document]] to which apply the rule.
 * @tparam T type representing the [[Result]] type.
 */
class ScraperActor[D <: Document, T](val scrapeRule: D => Iterable[T]) extends Actor with Stash:

  import ScraperActor.*

  private def onPause(): Unit =
    context.become(onPaused(this, receive))

  private def applyRule(argument: D): Iterable[T] =
    scrapeRule(argument)

  /**
   * Function representing actor's behavior.
   * @return
   */
  def receive: Receive =
    case CommonMessages.Pause =>
      onPause()
    case CommonMessages.Resume =>
    case Messages.Scrape(doc: D) =>
      val result: Result[T] = Result(applyRule(doc))
      sender() ! Messages.SendPartialResult(result) // TODO: send to explorer when ready

/**
 * Companion object for the Scraper actor.
 */
object ScraperActor:

  def props[D <: Document, T](scrapeRule: D => Iterable[T]): Props = Props(new ScraperActor(scrapeRule))

  /**
   * Enum representing all [[ScraperActor]] messages.
   */
  enum Messages:
    case Scrape[D <: Document](doc: D)
    case SendPartialResult[T](result: Result[T])

  /**
   * Utility for scraper's rules based on selectBy attribute, given selectors specified.
   * Admissible values are id, tag, class and css.
   *
   * @param selectors a [[Seq]] of selectors used in scraper rule.
   * @param selectBy a selector to specify the rule.
   * @return the selected rule with specified selectors.
   */
  def scraperRule(selectors: Seq[String], selectBy: String): IterableFromDoc[ScrapeDocument, String] = (scraper: ScrapeDocument) =>
    selectBy match
      case "id" =>
        selectors.map(scraper.getElementById).map(_.text)
      case "tag" =>
        selectors.flatMap(scraper.getElementByTag).map(_.text)
      case "class" =>
        selectors.flatMap(scraper.getElementByClass).map(_.text)
      case "css" =>
        selectors.flatMap(scraper.select(_)).map(_.text)

  /**
   * A scraper rule based on elements' ids given.
   * @param ids a [[Seq]] of ids used in the rule.
   * @return the rule based on elements' ids.
   */
  def scraperIdSelectorRule(ids: Seq[String]): IterableFromDoc[ScrapeDocument, String] = (scraper: ScrapeDocument) =>
    ids.map(scraper.getElementById).map(_.text)

  /**
   * A scraper rule based on elements' tags given.
   *
   * @param tags a [[Seq]] of tags used in the rule.
   * @return the rule based on elements' tags.
   */
  def scraperTagSelectorRule(tags: Seq[String]): IterableFromDoc[ScrapeDocument, String] = (scraper: ScrapeDocument) =>
    tags.flatMap(scraper.getElementByTag).map(_.text)

  /**
   * A scraper rule based on elements' classes given.
   *
   * @param classesNames a [[Seq]] of classes used in the rule.
   * @return the rule based on elements' classes.
   */
  def scraperClassSelectorRule(classesNames: Seq[String]): IterableFromDoc[ScrapeDocument, String] = (scraper: ScrapeDocument) =>
    classesNames.flatMap(scraper.getElementByClass).map(_.text)

  /**
   * A scraper rule based on css selectors given.
   *
   * @param selectors a [[Seq]] of selectors used in the rule.
   * @return the rule based on css selectors.
   */
  def scraperCSSSelectorsRule(selectors: Seq[String]): IterableFromDoc[ScrapeDocument, String] = (scraper: ScrapeDocument) =>
    selectors.flatMap(scraper.select(_)).map(_.text)

  /**
   * A scraper rule based on regular expressions given.
   *
   * @param regex a [[Seq]] of regex used in the rule.
   * @return the rule based on regex.
   */
  def regexSelectorsRule(regex: Seq[String]): IterableFromDoc[RegExpExplorer, String] = (regExScraper: RegExpExplorer) =>
    regex.flatMap(regExScraper.find)

  /**
   * A scraper rule which concatenates a rule with selectors and one with regex.
   *
   * @param selectors the [[Seq]] of selectors used in the first rule.
   * @param selectBy a selector to specify the rule.
   * @param regex the [[Seq]] of regex to apply in the second rule.
   * @return a rule that can be applied to [[ScrapeDocument]] to obtain an [[Iterable]] of String as a result of the concatenated rules.
   */
  def scrapeSelectThenRegex(selectors: Seq[String], selectBy: String)(regex: Seq[String]): IterableFromDoc[ScrapeDocument, String] = (scraper: ScrapeDocument) =>
    val doc = new Document(
      scraperRule(selectors, selectBy)(scraper).reduceOption(_ concat _).getOrElse(""),
      scraper.url)
      with RegExpExplorer
    regexSelectorsRule(regex)(doc)
