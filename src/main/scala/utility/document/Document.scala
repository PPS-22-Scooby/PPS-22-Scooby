package org.unibo.scooby
package utility.document
import org.jsoup.Jsoup
import org.unibo.scooby.utility.http.URL

trait Parser[T]:
  def parse(s: String): T

case class Document(content: String, url: URL)
