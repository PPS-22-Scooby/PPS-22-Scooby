package org.unibo.scooby
package utility.document

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

import java.net.URL

class DocumentTest extends AnyFlatSpec with should.Matchers:

    "A Document" should "be able to store a HTML string and URL" in:
      val html = "<html>" +
                    "<head>" +
                      "<title> Sample Title </title>" +
                    "</head>" +
                  "<body></body>" +
                "</html>"
      val URL = new URL("http://www.example.com")
      val document = Document(html, URL)
      document.content should be (html)
      document.url should be (URL)


