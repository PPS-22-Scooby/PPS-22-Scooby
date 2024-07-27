package org.unibo.scooby
package dsl

import core.exporter.Exporter.Formats
import core.scraper.ScraperPolicies.ScraperPolicy
import core.scraper.{Result, ScraperPolicies}

import dsl.Export.ExportOps.FormatType
import dsl.util.ScoobyTest

import utility.document.ScrapeDocument
import utility.document.html.HTMLElement
import utility.http.HttpError
import utility.http.api.Calls.GET

import scala.compiletime.uninitialized

class DSLExporterTest extends ScoobyTest:

  var expected: String = uninitialized
  var expectedIterable: Iterable[String] = uninitialized

  val scrapePolicy: ScraperPolicy[HTMLElement] = doc => doc.getAllElements
  val elemPolicy: HTMLElement => String = _.tag
  val resultPolicy: Iterable[HTMLElement] => Result[String] = it => Result(it.get(tag))
  val format: FormatType = Text

  "Exporter with batch write on file" should "correctly write on file final result" in:

    val docEither: Either[HttpError, ScrapeDocument] = GET(baseURL)
    val results = scrapePolicy(docEither.getOrElse(fail()))

    expected = if format == FormatType.Text
      then Formats.string(resultPolicy(results))
      else Formats.json.apply(resultPolicy(results))

    val filePath = path.resolve("exporter.txt")

    mockedScooby:
      exports:
        batch:
          strategy:
            results get tag output:
              toFile(filePath.toString) withFormat format
    .scrapeExportInspectFileContains(baseURL, filePath, expected, scrapePolicy)


  "Exporter with stream write on console" should "correctly output all result's items" in:

    val docEither: Either[HttpError, ScrapeDocument] = GET(baseURL)
    expectedIterable = scrapePolicy(docEither.getOrElse(fail())).map(elemPolicy)

    val separator = if format == FormatType.Text then System.lineSeparator() else ","

    mockedScooby:
      exports:
        streaming:
          results get tag  output:
            toConsole withFormat format
    .scrapeExportInspectConsoleContains(baseURL, expectedIterable, separator, scrapePolicy)
