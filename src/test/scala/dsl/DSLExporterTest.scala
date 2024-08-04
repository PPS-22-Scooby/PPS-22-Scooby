package org.unibo.scooby
package dsl

import org.unibo.scooby.core.exporter.Exporter.Formats
import org.unibo.scooby.core.scraper.ScraperPolicies.ScraperPolicy
import org.unibo.scooby.core.scraper.{Result, ScraperPolicies}
import org.unibo.scooby.dsl.util.ScoobyTest
import org.unibo.scooby.utility.document.ScrapeDocument
import org.unibo.scooby.utility.document.html.HTMLElement
import org.unibo.scooby.utility.http.HttpError
import org.unibo.scooby.utility.http.api.Calls.GET

import scala.compiletime.uninitialized

class DSLExporterTest extends ScoobyTest:

  var expected: String = uninitialized
  var expectedIterable: Iterable[String] = uninitialized

  val scrapePolicy: ScraperPolicy[HTMLElement] = doc => doc.getAllElements
  val elemPolicy: HTMLElement => String = _.tag
  val resultPolicy: Iterable[HTMLElement] => Result[String] = it => Result(it.get(tag))

  "Exporter with batch write on file" should "correctly write on file final result" in:

    val docEither: Either[HttpError, ScrapeDocument] = GET(baseURL)
    val results = scrapePolicy(docEither.getOrElse(fail()))

    expected = Formats.string(resultPolicy(results))

    val filePath = path.resolve("exporter.txt")

    mockedScooby:
      exports:
        batch:
          strategy:
            results get tag output:
              toFile(filePath.toString) withFormat text
    .scrapeExportInspectFileContains(baseURL, filePath, expected, scrapePolicy)


  "Exporter with stream write on console" should "correctly output all result's items" in:

    val docEither: Either[HttpError, ScrapeDocument] = GET(baseURL)
    expectedIterable = scrapePolicy(docEither.getOrElse(fail())).map(elemPolicy)

    val separator = System.lineSeparator()

    mockedScooby:
      exports:
        streaming:
          results get tag output:
            toConsole withFormat text
    .scrapeExportInspectConsoleContains(baseURL, expectedIterable, separator, scrapePolicy)
