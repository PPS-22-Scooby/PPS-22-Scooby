package org.unibo.scooby
package utility.document

import java.net.URL
import scala.util.matching.Regex
import html.*

trait RegExpExplorer extends Document:
  def find(regExp: String): Seq[String] =
    group(regExp.r.findAllMatchIn(content))

  def group(toGroup: Iterator[Regex.Match]): Seq[String] = toGroup.map(_.group(0)).toSeq

trait LinkExplorer extends RegExpExplorer:
  def frontier: Seq[String] = find("""<a\b[^>]*href="([^#][^"]*)""")

  override def group(toGroup: Iterator[Regex.Match]): Seq[String] = toGroup.map(_.group(1)).toSeq

trait HtmlExplorer extends Document:
  import html.given
  protected lazy val htmlDocument: HTMLDom = parseDocument
  private def parseDocument(using parser: Parser[HTMLDom]): HTMLDom =
    parser.parse(content)


trait SelectorExplorer extends HtmlExplorer:
  def select(selectors: String*): Seq[HTMLElement] =
    htmlDocument.select(selectors*)


class CrawlDocument(content: String, url: URL) extends Document(content, url) with LinkExplorer

