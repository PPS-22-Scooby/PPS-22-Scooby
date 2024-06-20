package org.unibo.scooby
package utility.document

import java.net.URL

trait Parser[T]:
  def parse(s: String): T

case class Document(content: String, url: URL)

