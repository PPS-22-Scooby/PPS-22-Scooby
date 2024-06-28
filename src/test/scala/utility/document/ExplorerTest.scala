package org.unibo.scooby
package utility.document

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

import java.net.URL
import scala.Seq

class ExplorerTest extends AnyFlatSpec with should.Matchers:

  val html = """<html>
                |<body>
                | <p id="test">Test</p>
                | <p>Test2</p>
                | <a id="link1" href="/example">Ping</a>
                | <a id="link2" class="link2" href="www.google.com">Pong</a>
                | <a href="https://blog.example.com">Pang</a>
                | <a href="https://www.blog.example.com">Pung</a>
                | </body>
                | </html>
                |""".stripMargin
  val URL = new URL("https://www.example.com")

  "A Document with RegExpExplorer" should "find element inside string given Regular Expression" in:
    val document = new Document(html, URL) with RegExpExplorer
    val regExp = "(?<=<p[^>]*>).*?(?=</p>)"
    document.find(regExp) should be (Seq("Test", "Test2"))

  "A Document with RegExpExplorer" should "find no element inside string given a non matching Regular Expression" in:
    val document = new Document(html, URL) with RegExpExplorer
    val regExp = "(?<=<span>).*?(?=</span>)"
    document.find(regExp) should be (empty)

  "A Document with LinkExplorer" should "find the links inside an HTML string" in:
    val document = new Document(html, URL) with LinkExplorer
    document.frontier should be(Seq("/example", "www.google.com",
      "https://blog.example.com", "https://www.blog.example.com"))

  "A document with HtmlExplorer" should "create an HTML document" in:
    val document = new Document(html, URL) with SelectorExplorer
    document.select("a").length should be(4)
    document.select("a").map(_.text) should be(Seq("Ping", "Pong", "Pang", "Pung"))

  "A document with HtmlExplorer" should "create an HTML document and extract elements by id" in:
    val document = new Document(html, URL) with CommonHTMLExplorer
    document.getElementById("test").text should be("Test")
    document.getElementById("link1").attr("href") should be("/example")
    document.getElementById("link1").tag should be("a")
    document.getElementById("link1").outerHtml should be("""<a id="link1" href="/example">Ping</a>""")

  "A document with HtmlExplorer" should "create an HTML document and extract elements by tag" in:
    val document = new Document(html, URL) with CommonHTMLExplorer
    document.getElementByTag("p").length should be(2)
    document.getElementByTag("p").map(_.text) should be(Seq("Test", "Test2"))

  "A document with HtmlExplorer" should "create an HTML document and extract elements by class" in:
    val document = new Document(html, URL) with CommonHTMLExplorer
    document.getElementByClass("test").length should be(0)
    document.getElementByClass("link2").length should be(1)
    document.getElementByClass("link2").map(_.text) should be(Seq("Pong"))
    document.getElementByClass("link2").map(_.attr("href")) should be(Seq("www.google.com"))
    document.getElementByClass("link2").map(_.tag) should be(Seq("a"))
    document.getElementByClass("link2").map(_.outerHtml) should be(Seq("""<a id="link2" class="link2" href="www.google.com">Pong</a>"""))

    