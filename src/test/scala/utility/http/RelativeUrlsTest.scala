package org.unibo.scooby
package utility.http

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class RelativeUrlsTest extends AnyFlatSpec with should.Matchers:

  private val invalidUrls: Seq[String] = Seq("htp://example.com",
    "http:\\example.com",
    "http://",
    "http:// example.com",
    "http://example:a",
    "http://example<com",
    "http://|com",
    "://example.com",
    "http://exa mple.com",
    "http://example.com:-80)")

  private val relativeUrls: Seq[String] = Seq(
    "/about",
    "/images/logo.png",
    "../docs/readme.md",
    "./contact",
    "products/list",
    "../assets/styles.css",
    "./scripts/main.js",
    "help/faq",
    "../index.html",
    "www.google.com/team/profile"
  )

  private val absoluteUrls: Seq[String] = Seq(
    "https://www.google.com",
    "http://example.com:8080/path?query#fragment",
    "https://www.wikipedia.org",
    "http://localhost:3000",
    "https://example.com/resource",
    "http://example.org",
    "https://www.example.net/index.html",
    "http://example.com/images/logo.png",
    "https://subdomain.example.com",
    "http://example.com/help/faq"
  )

  "Invalid URL strings" should "return an invalid URL" in:
    invalidUrls.map(URL(_)).foreach(_ shouldBe a[URL.Invalid])

  "Relative URL strings" should "be recognized as Relative URLs" in:
    relativeUrls.map(URL(_)).foreach(_ shouldBe a[URL.Relative])

  "Absolute URL strings" should "be recognized as Absolute URLs" in:
    absoluteUrls.map(URL(_)).foreach(_ shouldBe a[URL.Absolute])

  "Relative URLs" should "be resolved into valid Absolute URLs" in:
    import utility.http.URL.toUrl
    val absolutePath = URL("https://validurl.com/test/example")
    val resolved = Seq(
      "https://validurl.com/about",
      "https://validurl.com/images/logo.png",
      "https://validurl.com/docs/readme.md",
      "https://validurl.com/test/contact",
      "https://validurl.com/test/products/list",
      "https://validurl.com/assets/styles.css",
      "https://validurl.com/test/scripts/main.js",
      "https://validurl.com/test/help/faq",
      "https://validurl.com/index.html",
      "https://validurl.com/test/www.google.com/team/profile"
    )
    relativeUrls.map(URL(_).resolve(absolutePath)).zipWithIndex.foreach:
      (url, index) =>
      url shouldBe resolved(index).toUrl
