package org.unibo.scooby
package core.scraper

import utility.document.{Document, RegExpExplorer, ScrapeDocument}
import utility.message.CommonMessages.{CommonMessages, onPaused}

import akka.actor.{Actor, ActorSystem, Props, Stash}

type IterableFromDoc[D <: Document, T] = D => Iterable[T]

class ScraperActor[D <: Document, T](val scrapeRule: D => Iterable[T]) extends Actor with Stash:

  import ScraperActor.*

  private def onPause(): Unit =
    context.become(onPaused(this, receive))

  private def applyRule(argument: D): Iterable[T] =
    scrapeRule(argument)

  def receive: Receive =
    case CommonMessages.Pause =>
      onPause()
    case CommonMessages.Resume =>
    case Messages.Scrape(doc: D) =>
      val result: Result[T] = Result(applyRule(doc))
      sender() ! Messages.SendPartialResult(result) // TODO: send to explorer when ready


object ScraperActor:

  def props[D <: Document, T](scrapeRule: D => Iterable[T]): Props = Props(new ScraperActor(scrapeRule))

  enum Messages:
    case Scrape[D <: Document](doc: D)
    case SendPartialResult[T](result: Result[T])

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

  def scraperIdSelectorRule(ids: Seq[String]): IterableFromDoc[ScrapeDocument, String] = (scraper: ScrapeDocument) =>
    ids.map(scraper.getElementById).map(_.text)

  def scraperTagSelectorRule(tags: Seq[String]): IterableFromDoc[ScrapeDocument, String] = (scraper: ScrapeDocument) =>
    tags.flatMap(scraper.getElementByTag).map(_.text)

  def scraperClassSelectorRule(classNames: Seq[String]): IterableFromDoc[ScrapeDocument, String] = (scraper: ScrapeDocument) =>
    classNames.flatMap(scraper.getElementByClass).map(_.text)

  def scraperCSSSelectorsRule(selectors: Seq[String]): IterableFromDoc[ScrapeDocument, String] = (scraper: ScrapeDocument) =>
    selectors.flatMap(scraper.select(_)).map(_.text)

  def regexSelectorsRule(regex: Seq[String]): IterableFromDoc[RegExpExplorer, String] = (regExScraper: RegExpExplorer) =>
    regex.flatMap(regExScraper.find)

  def scrapeSelectThenRegex(selectors: Seq[String], selectBy: String)(regex: Seq[String]): IterableFromDoc[ScrapeDocument, String] = (scraper: ScrapeDocument) =>
    val doc = new Document(
      scraperRule(selectors, selectBy)(scraper).reduceOption(_ concat _).getOrElse(""),
      scraper.url)
      with RegExpExplorer
    regexSelectorsRule(regex)(doc)
