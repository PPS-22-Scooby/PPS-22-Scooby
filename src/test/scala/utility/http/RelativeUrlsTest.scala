package org.unibo.scooby
package utility.http

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class RelativeUrlsTest extends AnyFlatSpec with should.Matchers:

  "Invalid URL strings" should "return an invalid URL" in:
    val invalid = Seq("htp://example.com",
      "http:///example.com",
      "http://",
      "http:// example.com",
      "http://example",
      "http://example..com",
      "http://.com",
      "://example.com",
      "http://exa mple.com",
      "http://example.com:-80)")

    invalid.map(URL(_).isValid).reduce(_ && _) should be(false)
