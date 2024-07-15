package org.unibo.scooby
package utility.document

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import org.unibo.scooby.utility.http.URL

import scala.Seq

class ExplorerTest extends AnyFlatSpec with should.Matchers:

  val html = """<html>
                |<body>
                | <p id="test">Test</p>
                | <p>Test2</p>
                | <a id="link1" href="./example.html">Ping</a>
                | <a id="link2" class="link2" href="file.html">Pong</a>
                | <a href="https://blog.example.com">Pang</a>
                | <a href="https://www.blog.example.com">Pung</a>
                | </body>
                | </html>
                |""".stripMargin
  val url = URL("https://www.example.com/path")

  "A Document with RegExpExplorer" should "find element inside string given Regular Expression" in:
    val document = new Document(html, url) with RegExpExplorer
    val regExp = "(?<=<p[^>]*>).*?(?=</p>)"
    document.find(regExp) should be (Seq("Test", "Test2"))

  "A Document with RegExpExplorer" should "find no element inside string given a non matching Regular Expression" in:
    val document = new Document(html, url) with RegExpExplorer
    val regExp = "(?<=<span>).*?(?=</span>)"
    document.find(regExp) should be (empty)

  "A Document with LinkExplorer" should "find the links inside an HTML string" in:
    val document = new Document(html, url) with LinkExplorer
    document.frontier.map(_.toString) should be(Iterable("https://www.example.com/example.html",
      "https://www.example.com/file.html", "https://blog.example.com", "https://www.blog.example.com"))

  "A document with HtmlExplorer" should "create an HTML document" in:
    val document = new Document(html, url) with SelectorExplorer
    document.select("a").length should be(4)
    document.select("a").map(_.text) should be(Seq("Ping", "Pong", "Pang", "Pung"))

  "A document with HtmlExplorer" should "create an HTML document and extract elements by id" in:
    val document = new Document(html, url) with CommonHTMLExplorer
    document.getElementById("test").fold("")(_.text) should be("Test")
    document.getElementById("link1").fold("")(_.attr("href")) should be("./example.html")
    document.getElementById("link1").fold("")(_.tag) should be("a")
    document.getElementById("link1").fold("")(_.outerHtml) should be("""<a id="link1" href="./example.html">Ping</a>""")

  "A document with HtmlExplorer" should "create an HTML document and extract elements by tag" in:
    val document = new Document(html, url) with CommonHTMLExplorer
    document.getElementByTag("p").length should be(2)
    document.getElementByTag("p").map(_.text) should be(Seq("Test", "Test2"))

  "A document with HtmlExplorer" should "create an HTML document and extract elements by class" in:
    val document = new Document(html, url) with CommonHTMLExplorer
    document.getElementByClass("test").length should be(0)
    document.getElementByClass("link2").length should be(1)
    document.getElementByClass("link2").map(_.text) should be(Seq("Pong"))
    document.getElementByClass("link2").map(_.attr("href")) should be(Seq("file.html"))
    document.getElementByClass("link2").map(_.tag) should be(Seq("a"))
    document.getElementByClass("link2").map(_.outerHtml) should be(Seq("""<a id="link2" class="link2" href="file.html">Pong</a>"""))

    