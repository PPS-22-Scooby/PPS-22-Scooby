package org.unibo.scooby
package core.scraper

import utility.document.{Document, RegExpExplorer, ScrapeDocument}
import utility.message.CommonMessages.{CommonMessages, onPaused}

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.{Actor, Props, Stash}
import core.exporter.ExporterCommands

/**
 * A type representing a function that extract an [[Iterable]] used to build [[DataResult]] from a [[Document]]
 *
 * @tparam D a type which is a subtype of [[Document]] type.
 * @tparam T a generic type which represents the expected [[DataResult]] type.
 */
type ScraperPolicy[D <: Document, T] = D => Iterable[T]

/**
 * Class representing Scraper actor.
 *
 * @param scrapeRule the scraping rule the actor uses.
 * @tparam D the type representing the [[Document]] to which apply the rule.
 * @tparam T type representing the [[DataResult]] type.
 */
class Scraper[D <: Document, T](val exporter: ActorRef[ExporterCommands], val scrapeRule: ScraperPolicy[D, T]):

  import Scraper.ScraperCommands
  import core.exporter.ExporterCommands

  def idle(): Behavior[ScraperCommands] =
    Behaviors.setup : context =>
      Behaviors.receiveMessage :
        case ScraperCommands.Scrape(doc: D) =>
          val res = resultFromRule(doc)
          exporter ! ExporterCommands.Export(res)
          Behaviors.same
        case _ => Behaviors.same


  private def resultFromRule(argument: D): Result[T] =
    Result(scrapeRule(argument))

/**
 * Companion object for the Scraper actor.
 */
object Scraper:

  def apply[D <: Document, T](exporter: ActorRef[ExporterCommands], scrapeRule: ScraperPolicy[D, T]): Behavior[ScraperCommands] =
    Behaviors.setup {
      context => new Scraper(exporter, scrapeRule).idle()
    }

  /**
   * Enum representing all [[Scraper]] messages.
   */
  enum ScraperCommands:
    case Scrape[D <: Document](doc: D)
    case SendPartialResult[T](result: DataResult[T])

  /**
   * Utility for scraper's rules based on selectBy attribute, given selectors specified.
   * Admissible values are id, tag, class and css.
   *
   * @param selectors a [[Seq]] of selectors used in scraper rule.
   * @param selectBy a selector to specify the rule.
   * @return the selected rule with specified selectors.
   */
  def scraperRule(selectors: Seq[String], selectBy: String): ScraperPolicy[ScrapeDocument, String] = (scraper: ScrapeDocument) =>
    selectBy match
      case "id" =>
        selectors.map(scraper.getElementById).map(_.outerHtml)
      case "tag" =>
        selectors.flatMap(scraper.getElementByTag).map(_.outerHtml)
      case "class" =>
        selectors.flatMap(scraper.getElementByClass).map(_.outerHtml)
      case "css" =>
        selectors.flatMap(scraper.select(_)).map(_.outerHtml)
      case _ =>
        throw Error(s"Not yet implemented rule by $selectBy")

  /**
   * A scraper rule based on elements' ids given.
   * @param ids a [[Seq]] of ids used in the rule.
   * @return the rule based on elements' ids.
   */
  def scraperIdSelectorRule(ids: Seq[String]): ScraperPolicy[ScrapeDocument, String] = (scraper: ScrapeDocument) =>
    ids.map(scraper.getElementById).map(_.text)

  /**
   * A scraper rule based on elements' tags given.
   *
   * @param tags a [[Seq]] of tags used in the rule.
   * @return the rule based on elements' tags.
   */
  def scraperTagSelectorRule(tags: Seq[String]): ScraperPolicy[ScrapeDocument, String] = (scraper: ScrapeDocument) =>
    tags.flatMap(scraper.getElementByTag).map(_.text)

  /**
   * A scraper rule based on elements' classes given.
   *
   * @param classesNames a [[Seq]] of classes used in the rule.
   * @return the rule based on elements' classes.
   */
  def scraperClassSelectorRule(classesNames: Seq[String]): ScraperPolicy[ScrapeDocument, String] = (scraper: ScrapeDocument) =>
    classesNames.flatMap(scraper.getElementByClass).map(_.text)

  /**
   * A scraper rule based on css selectors given.
   *
   * @param selectors a [[Seq]] of selectors used in the rule.
   * @return the rule based on css selectors.
   */
  def scraperCSSSelectorsRule(selectors: Seq[String]): ScraperPolicy[ScrapeDocument, String] = (scraper: ScrapeDocument) =>
    selectors.flatMap(scraper.select(_)).map(_.text)

  /**
   * A scraper rule based on regular expressions given.
   *
   * @param regex a [[Seq]] of regex used in the rule.
   * @return the rule based on regex.
   */
  def regexSelectorsRule(regex: Seq[String]): ScraperPolicy[RegExpExplorer, String] = (regExScraper: RegExpExplorer) =>
    regex.flatMap(regExScraper.find)

  /**
   * A scraper rule which concatenates a rule with selectors and one with regex.
   *
   * @param selectors the [[Seq]] of selectors used in the first rule.
   * @param selectBy a selector to specify the rule.
   * @param regex the [[Seq]] of regex to apply in the second rule.
   * @return a rule that can be applied to [[ScrapeDocument]] to obtain an [[Iterable]] of String as a result of the concatenated rules.
   */
  def scrapeSelectThenRegex(selectors: Seq[String], selectBy: String)(regex: Seq[String]): ScraperPolicy[ScrapeDocument, String] = (scraper: ScrapeDocument) =>
    val doc = new Document(
      scraperRule(selectors, selectBy)(scraper).reduceOption(_.concat(_)).getOrElse(""),
      scraper.url)
      with RegExpExplorer
    regexSelectorsRule(regex)(doc)
