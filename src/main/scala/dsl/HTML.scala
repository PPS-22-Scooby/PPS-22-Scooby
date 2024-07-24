package org.unibo.scooby
package dsl

import utility.document.html.HTMLElement

import utility.document.{CommonHTMLExplorer, Document, RegExpExplorer, SelectorExplorer}

object HTML:
  import _root_.dsl.syntax.catchRecursiveCtx

  /**
   * Type alias for Rule definition scope (i.e. "rule: ...")
   */
  private type RuleDefinitionScope = HTMLElement ?=> Boolean
  /**
   * Type alias for a rule produced with the "rule" keyword
   */
  private type HTMLElementRule = HTMLElement => Boolean

  /**
   * Utility keyword to specify a rule that the obtained HTML
   * elements must attained to. Usable inside blocks like "scrape" or "crawl"
   *
   * @param block definition of the rule
   * @return a rule that filters HTML elements
   */
  inline def rule(block: RuleDefinitionScope): HTMLElementRule =
    catchRecursiveCtx[HTMLElement]("rule")
    el =>
      given HTMLElement = el
      block

  /**
   * Keyword to retrieve the HTML element of this scope (inside "rule" block)
   * @param el element of this scope
   * @return the HTML element of this scope
   */
  def element(using el: HTMLElement): HTMLElement = el

  /**
   * Type alias for the functions transforming a HTML element into a String
   */
  private type ElementToString = HTMLElement => String

  /**
   * Retrieves tag from an [[HTMLElement]]
   *
   * @return the tag as [[String]].
   */
  def tag: ElementToString = _.tag

  /**
   * Retrieves text from an [[HTMLElement]]
   *
   * @return text as [[String]].
   */
  def text: ElementToString = _.text

  /**
   * Retrieves outer Html text from an [[HTMLElement]]
   *
   * @return outer Html text as [[String]].
   */
  def outerText: ElementToString = _.outerHtml

  /**
   * Retrieves value of the specified attribute of an [[HTMLElement]].
   *
   * @param attribute the attribute to read the value.
   * @return the value of the attribute as [[String]].
   */
  def attr(attribute: String): ElementToString = _.attr(attribute)

  /**
   * Utility keyword to retrieve the classes of this HTML element.
   * @return an [[Iterable]] of String of the names of the classes.
   */
  def classes: HTMLElement => Iterable[String] = _.classes

  /**
   * Utility keyword to retrieve the attributes of this HTML element.
   * @return an [[Iterable]] of `(String, String)` representing (attribute name, value)
   */
  def attributes: HTMLElement => Iterable[(String, String)] = _.attributes

  /**
   * Utility keyword to retrieve the "id" of the HTML element.
   * @return a mapping from HTML to its String id
   */
  def id: ElementToString = _.id


  /**
   * Utility keyword to retrieve the HTML elements contained in the HTML document.
   *
   * @param documentContext document present in this context
   * @tparam D type of document present in this scope (e.g. CrawlDocument, ScrapeDocument ecc.)
   * @return an [[Iterable]] of all the HTML elements contained in the document
   */
  def elements[D <: Document & CommonHTMLExplorer](using documentContext: D): Iterable[HTMLElement] =
    documentContext.getAllElements

  /**
   * Utility keyword to retrieve the document from inside a scope that uses a document (e.g. scrape, crawl)
   * @param documentContext document present in this context
   * @tparam D type of document present in this scope (e.g. CrawlDocument, ScrapeDocument ecc.)
   * @return the document of the context
   */
  def document[D <: Document & CommonHTMLExplorer](using documentContext: D): D = documentContext

  /**
   * Utility keyword to retrieve all the string matches of the provided regular expression.
   * @param regExp regular expression to match
   * @param documentContext document present in this context (must be a Document with regular expression capabilities)
   * @tparam D type of document present in this scope (e.g. CrawlDocument, ScrapeDocument ecc.)
   * @return an [[Iterable]] of [[String]] matching the expression inside the content of the document
   */
  def matchesOf[D <: Document & RegExpExplorer](regExp: String)(using documentContext: D): Iterable[String] =
    documentContext.find(regExp)

  /**
   * Utility keyword to retrieve all the HTML elements matching the provided selectors.
   * @param selectors selectors concatenated with ","
   * @param documentContext document present in this context
   * @tparam D type of document present in this scope (e.g. CrawlDocument, ScrapeDocument ecc.)
   * @return an [[Iterable]] of the [[HTMLElement]]s matching the selectors
   */
  def select[D <: Document & SelectorExplorer](selectors: String*)(using documentContext: D): Iterable[HTMLElement] =
    documentContext.select(selectors *)

  /**
   * Rule specifying the HTML elements with a specific tag.
   * @param tag the tag of the HTML elements
   * @return a predicate for HTML elements
   */
  infix def haveTag(tag: String): HTMLElementRule = _.tag == tag

  /**
   * Rule specifying the HTML elements with a specific class.
   * @param cssClass the class of the HTML elements
   * @return a predicate for HTML elements
   */
  infix def haveClass(cssClass: String): HTMLElementRule = _.classes.contains(cssClass)

  /**
   * Rule specifying the HTML elements with a specific id.
   * @param id the id of the HTML elements
   * @return a predicate for HTML elements
   */
  infix def haveId(id: String): HTMLElementRule = _.id == id

  /**
   * Rule specifying the HTML elements with a specific attribute.
   * @param attributeName the attribute name of the HTML elements
   * @return a predicate for HTML elements
   */
  infix def haveAttribute(attributeName: String): HTMLElementRule = _.attr(attributeName).nonEmpty

  /**
   * Rule specifying the HTML elements with a specific attribute and related value.
   * @param attributeName the attribute name of the HTML elements
   * @param attributeValue the value of the requested attribute
   * @return a predicate for HTML elements
   */
  infix def haveAttributeValue(attributeName: String, attributeValue: String): HTMLElementRule =
    _.attr(attributeName) == attributeValue
