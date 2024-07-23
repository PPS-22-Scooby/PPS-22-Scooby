package org.unibo.scooby
package utility.document

import utility.document.html.*
import utility.http.URL

import scala.util.matching.Regex

/**
 * Trait that provides functionality to explore a document using regular expressions.
 */
trait RegExpExplorer extends Document:

  /**
   * Finds all matches of the given regular expression in the document's content.
   *
   * @param regExp
   *   the regular expression to match
   * @return
   *   an sequence of all matches
   */
  def find(regExp: String): Seq[String] =
    group(regExp.r.findAllMatchIn(content))

  /**
   * Groups all matches of a regular expression.
   *
   * @param toGroup
   *   the matches to group
   * @return
   *   a sequence of grouped matches
   */
  def group(toGroup: Iterator[Regex.Match]): Seq[String] = toGroup.map(_.group(0)).toSeq

/**
 * Trait that provides functionality to explore links in a document.
 */
trait LinkExplorer extends RegExpExplorer, HtmlExplorer:

  /**
   * Finds all links in "href" attribute in the document's content.
   *
   * @return
   *   a sequence of all links
   */
  def frontier: Seq[URL] = find("""<a\b[^>]*href="([^#][^"]*)""").map(URL(_).resolve(url)).filter(_.isValid).toSeq

  override def group(toGroup: Iterator[Regex.Match]): Seq[String] = toGroup.map(_.group(1)).toSeq

trait EnhancedLinkExplorer extends HtmlExplorer:
  /**
   * Retrieves all the links from the HTML document.
   *
   * @return a sequence of URLs representing the links in the document.
   */
  def getAllLinkOccurrences: Seq[URL] = htmlDocument.getAllLinkOccurrences


/**
 * Trait that provides functionality to explore an HTML document.
 */
trait HtmlExplorer extends Document:
  import html.given
  protected lazy val htmlDocument: HTMLDom = parseDocument

  /**
   * Parses the document's content into an HTMLDom instance.
   *
   * @param parser
   *   the parser to use
   * @return
   *   an HTMLDom instance representing the document's content
   */
  private def parseDocument(using parser: Parser[HTMLDom]): HTMLDom =
    parser.parse(content)

/**
 * Trait that provides functionality to select elements from an HTML document using CSS selectors.
 */
trait SelectorExplorer extends HtmlExplorer:

  /**
   * Selects elements from the HTML document using CSS selectors.
   *
   * @param selectors
   *   the CSS selectors to use
   * @return
   *   a sequence of selected elements
   */
  def select(selectors: String*): Seq[HTMLElement] =
    htmlDocument.select(selectors*)

/**
 * Trait that provides common functionality to explore an HTML document.
 */
trait CommonHTMLExplorer extends HtmlExplorer:

  /**
   * Gets an element from the HTML document by its ID.
   *
   * @param id
   *   the ID of the element
   * @return
   *   an [[Option]] which encapsulates the element with the given ID
   */
  def getElementById(id: String): Option[HTMLElement] = htmlDocument.getElementById(id)

  /**
   * Gets elements from the HTML document by their tag name.
   *
   * @param tag
   *   the tag name
   * @return
   *   a sequence of elements with the given tag name
   */
  def getElementsByTag(tag: String): Seq[HTMLElement] = htmlDocument.getElementByTag(tag)

  /**
   * Gets elements from the HTML document by their class name.
   *
   * @param className
   *   the class name
   * @return
   *   a sequence of elements with the given class name
   */
  def getElementsByClass(className: String): Seq[HTMLElement] = htmlDocument.getElementByClass(className)

  /**
    * Gets all the elements from the HTML document
    *
    * @return a sequence containing all the HTML elements of the document
    */
  def getAllElements: Seq[HTMLElement] = htmlDocument.allElements

class CrawlDocument(content: String, url: URL) extends Document(content, url)
      with LinkExplorer
      with EnhancedLinkExplorer

class ScrapeDocument(content: String, url: URL) extends Document(content, url)
      with SelectorExplorer
      with CommonHTMLExplorer
      with RegExpExplorer
