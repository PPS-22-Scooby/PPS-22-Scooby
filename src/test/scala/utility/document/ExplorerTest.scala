package org.unibo.scooby
package utility.document

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

import java.net.URL
import scala.Seq

class ExplorerTest extends AnyFlatSpec with should.Matchers:

  "A Document with RegExpExplorer" should "find element inside string given Regular Expression" in:
    val html = "<html><body><p>Test</p> <p>Test2</p> </body></html>"
    val URL = new URL("https://www.example.com")
    val document = new Document(html, URL) with RegExpExplorer
    val regExp = "(?<=<p>).*?(?=</p>)"
    document.find(regExp) should be (Seq("Test", "Test2"))

  "A Document with RegExpExplorer" should "find no element inside string given a non matching Regular Expression" in:
    val html = "<html><body></body></html>"
    val URL = new URL("https://www.example.com")
    val document = new Document(html, URL) with RegExpExplorer
    val regExp = "(?<=<p>).*?(?=</p>)"
    document.find(regExp) should be (empty)

  "A Document with LinkExplorer" should "find the links inside an HTML string" in:
    val html =
      """<html>
          | <a href="/example"></a>
          | <a href="www.google.com"></a>
          | <a href="https://blog.example.com"></a>
          | <a href="https://www.blog.example.com"></a>
        |</html>
        |""".stripMargin
    val URL = new URL("https://www.example.com")
    val document = new Document(html, URL) with LinkExplorer
    document.frontier should be(Seq("/example", "www.google.com",
      "https://blog.example.com", "https://www.blog.example.com"))

  "A document with HtmlExplorer" should "create an HTML document" in:
    val html =
      """<html>
        | <a href="/example">Ping</a>
        | <a href="www.google.com">Pong</a>
        | <a href="https://blog.example.com">Pang</a>
        | <a href="https://www.blog.example.com">Prum</a>
        |</html>
        |""".stripMargin
    val URL = new URL("https://www.example.com")
    val document = new Document(html, URL) with SelectorExplorer
    document.select("a").length should be(4)
    document.select("a").map(_.text) should be(Seq("Ping", "Pong", "Pang","Prum"))
    