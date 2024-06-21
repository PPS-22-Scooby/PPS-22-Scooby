package org.unibo.scooby
package core.scraper.result

import io.cucumber.scala.{EN, ScalaDsl}
import core.scraper.Result

import io.cucumber.datatable.DataTable
import org.jsoup.Jsoup

class StepDefinitions extends ScalaDsl with EN:

  private val result: Result[String] = Result()

  trait Scraper[T] {
    def scrape(doc: String): T
  }

  Given("""I have Scrapers that elaborate documents with different filtering policies which generates different data <type> from the document""") : (dataTable: DataTable) =>
    val stringDummyScraper = new Scraper[String] {
      override def scrape(doc: String): String = {
        doc
      }
    }
    val mapDummyScraper = new Scraper[Map[String, String]] {
      override def scrape(doc: String): Map[String, String] = {
        val document = Jsoup.parse(doc)
        Map(
          "title" -> document.title(),
          "body" -> document.body().text()
        )
      }
    }

  And("""There are different <documents>""") : (docs: String) =>
    val documents = docs.toList
    print(documents)


  When("""The scraper starts filtering the document, obtaining data to aggregate""") : () =>
    print("test")

  Then("""It will obtain a <result>""") : (results: String) =>
    print("test")
