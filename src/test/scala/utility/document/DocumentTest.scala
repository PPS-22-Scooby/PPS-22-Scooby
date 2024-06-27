package org.unibo.scooby
package utility.document

import utility.http.URL

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should


class DocumentTest extends AnyFlatSpec with should.Matchers:

    "A Document" should "be able to store a HTML string and URL" in:
      val html = "<html>" +
                    "<head>" +
                      "<title> Sample Title </title>" +
                    "</head>" +
                  "<body></body>" +
                "</html>"
      val urlEither = URL("http://www.example.com")
      assert(urlEither.isRight)
      urlEither.fold(_ => fail("Illegal URL"), url =>
        val document = Document(html, url)
        document.content should be (html)
        document.url should be (url)
      )
      


