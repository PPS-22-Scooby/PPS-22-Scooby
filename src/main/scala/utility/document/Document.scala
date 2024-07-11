package org.unibo.scooby
package utility.document
import  utility.http.URL

trait Parser[T]:
  def parse(s: String): T

case class Document(content: String, url: URL)
