package org.unibo.scooby
package utility.document

import java.net.URL
import scala.util.matching.Regex


trait RegExpExplorer extends Document:
  def find(regExp: String, groupToSeq: Iterator[Regex.Match] => Seq[String] = _.map(_.group(0)).toSeq): Seq[String] =
    groupToSeq(regExp.r.findAllMatchIn(content))

trait LinkExplorer extends RegExpExplorer:
  def frontier: Seq[String] = find("""<a\b[^>]*href="([^#][^"]*)""",  _.map(_.group(1)).toSeq)

trait HtmlExplorer extends Document

trait SelectorExplorer extends HtmlExplorer


class CrawlDocument(content: String, url: URL) extends Document(content, url) with LinkExplorer
