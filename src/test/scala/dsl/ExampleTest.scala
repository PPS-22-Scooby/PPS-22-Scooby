package org.unibo.scooby
package dsl

import dsl.util.ScoobyTest
import utility.http.URL.toUrl

class ExampleTest extends ScoobyTest:

  "Example" should "be an explanatory example for the team" in:
    mockedScooby:
      crawl:
        url:
          baseURL / "level1.0.html"
        policy:
          hyperlinks
    .expectCrawledLinks(
      baseURL / "level2.0.html",
      baseURL / "level2.1.html",
      baseURL / "index.html",
      "http://example.com".toUrl
    )

  "Another example" should "be an even more explanatory example for the team" in:
    val filePath = path.resolve("temp.txt")

    mockedScooby:
      exports:
        batch:
          strategy:
            results get tag output:
              toFile(filePath.toString) withFormat Text

    .scrapeExportInspectFileContains(baseURL, filePath,
      "List(html, head, meta, meta, title, body, h1, p, div, h2, a, br, a, br, a, br, a)\n"
    )
