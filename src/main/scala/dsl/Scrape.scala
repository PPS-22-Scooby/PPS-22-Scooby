package org.unibo.scooby
package dsl

import core.exporter.Exporter.AggregationBehaviors
import core.scooby.SingleExporting
import core.scraper.ScraperPolicies.ScraperPolicy
import dsl.DSL.ConfigurationBuilder
import utility.document.ScrapeDocument

import monocle.syntax.all.*
import org.unibo.scooby.dsl.DSL.ScrapingResultSetting
import org.unibo.scooby.utility.document.CommonHTMLExplorer
import org.unibo.scooby.utility.document.Document
import org.unibo.scooby.utility.document.html.HTMLElement
import org.unibo.scooby.utility.document.RegExpExplorer
import org.unibo.scooby.utility.document.SelectorExplorer

object Scrape:

  def scrape[T](init: ScrapeDocument ?=> Iterable[T])(using builder: ConfigurationBuilder[T]): Unit =
    builder.configuration = builder.configuration.focus(_.scraperConfiguration.scrapePolicy).replace:
      doc =>
        given ScrapeDocument = doc
        init
    builder.scrapingResultSetting = ScrapingResultSetting[T]()
    
  def elements[T <: Document & CommonHTMLExplorer](using documentContext: T): Iterable[HTMLElement] =
    documentContext.getAllElements
  
  def element(using el: HTMLElement): HTMLElement = el
  
  def matchesOf[T <: Document & RegExpExplorer](regExp: String)(using documentContext: T): Iterable[String] =
    documentContext.find(regExp)

  def select[T <: Document & SelectorExplorer](selectors: String*)(using documentContext: T): Iterable[HTMLElement] =
    documentContext.select(selectors*)

  // def tag: HTMLElement => String = _.tag 

  def classes: HTMLElement => Iterable[String] = _.classes
  def attributes: HTMLElement => Iterable[(String, String)] = _.attributes
  def id: HTMLElement => String = _.id

  infix def dont[T](predicate: T => Boolean): T => Boolean = (elem) => !predicate(elem)

  infix def haveTag(tag: String): HTMLElement => Boolean = _.tag == tag
  infix def haveClass(cssClass: String): HTMLElement => Boolean = _.classes.contains(cssClass)
  infix def haveId(id: String): HTMLElement => Boolean = _.id == id
  infix def haveAttribute(attributeName: String): HTMLElement => Boolean = _.attr(attributeName).nonEmpty
  infix def haveAttributeValue(attributeName: String, attributeValue: String): HTMLElement => Boolean =
    _.attr(attributeName) == attributeValue

  infix def rule(init: HTMLElement ?=> Boolean): HTMLElement => Boolean =
    el =>
      given HTMLElement = el
      init
      
      
    

  extension[T] (x: Iterable[T])
    infix inline def including[S >: T](y: Iterable[S]): Iterable[S] = x concat y

    infix inline def that(predicate: T => Boolean): Iterable[T] = x filter predicate

  extension[T](x: Iterable[T])
    infix def get[A](f: T => A): Iterable[A] = x.map(f)

  

  extension[T] (x: T => Boolean)
    infix def and(y: T => Boolean): T => Boolean = (el) => x(el) && y(el)
    infix def &&(y: T => Boolean): T => Boolean = and(y)
    infix def or(y: T => Boolean): T => Boolean = (el) => x(el) || y(el)
    infix def ||(y: T => Boolean): T => Boolean = or(y)
