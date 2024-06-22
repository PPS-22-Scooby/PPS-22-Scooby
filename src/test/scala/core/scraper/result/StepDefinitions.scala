package org.unibo.scooby
package core.scraper.result

import io.cucumber.scala.{EN, ScalaDsl}
import core.scraper.Result
import core.scraper.Result.*

import io.cucumber.datatable.DataTable
import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import play.api.libs.json._
import scala.jdk.CollectionConverters._

class StepDefinitions extends ScalaDsl with EN:

  private var typeToUse: String = _
  private var document: String = _
  private var resultMap: Result[Map[String, String]] = _
  private var resultString: Result[String] = _
  private var resultMap1: Result[Map[String, String]] = _
  private var resultMap2: Result[Map[String, String]] = _
  private var resultString1: Result[String] = _
  private var resultString2: Result[String] = _
  private val nonMatchPattern: String = "non_match"

  trait Scraper[T] {
    def scrape(doc: String): T
  }

  Given("""^I have Scrapers that elaborate documents with different filtering policies which generates different data (.*)$""") : (run_type: String) =>
    typeToUse = run_type

  Given("""^I have 2 Scrapers that elaborate documents with the same filtering policies of (.*), which works on different documents$""") : (run_type: String) =>
    typeToUse = run_type

  And("""^There are different (.*)$""") : (doc: String) =>
    document = doc

  And("""^generated different (.*)$"""): (result: String) =>
    typeToUse match
      case "String" =>
        val res = Json.parse(result).validate[List[String]]
        res match {
          case JsSuccess(resList, _) =>
            resultString1 = new Result(Option(resList(0)))
            resultString2 = new Result(Option(resList(1)))
          case JsError(errors) =>
            println(errors)
        }
      case "Map[String, String]" =>
        val res = Json.parse(result).validate[List[Map[String, String]]]
        res match {
          case JsSuccess(resMap, _) =>
            resultMap1 = new Result(Option(resMap(0)))
            resultMap2 = new Result(Option(resMap(1)))
          case JsError(errors) =>
            println(errors)
        }

  When("""^The scraper starts filtering the document, obtaining data to aggregate$""") : () =>
    typeToUse match
      case "String" =>
        val scraper = new Scraper[String] {
          override def scrape(doc: String): String = {
            val document = Jsoup.parse(doc)
            Map(
              "a" -> document
                .getElementsByTag("a")
                .eachText()
                .asScala.toList
                .filterNot(_.contains(nonMatchPattern))
                .map("<a>" concat _ concat "</a>")
                .mkString(""),
              "div" -> document
                .getElementsByTag("div")
                .eachText()
                .asScala.toList
                .filterNot(_.contains(nonMatchPattern))
                .map("<div>" concat _ concat "</div>")
                .mkString(""),
            ).values.mkString("")
          }
        }
        resultString = new Result(Option(scraper.scrape(this.document)))

      case "Map[String, String]" =>
        val scraper = new Scraper[Map[String, String]] {
          override def scrape(doc: String): Map[String, String] = {
            val document = Jsoup.parse(doc)
            Map(
              "a" -> document
                .getElementsByTag("a")
                .eachText()
                .asScala.toList
                .filterNot(_.contains(nonMatchPattern))
                .map("<a>" concat _ concat "</a>")
                .mkString(""),
              "div" -> document
                .getElementsByTag("div")
                .eachText()
                .asScala.toList
                .filterNot(_.contains(nonMatchPattern))
                .map("<div>" concat _ concat "</div>")
                .mkString(""),
            )
          }
        }
        resultMap = new Result(Option(scraper.scrape(this.document)))

  When("""^The scrapers finished to scrape$""") : () =>
    println("Finish!")

  Then("""^It will obtain (.*)$""") : (result: String) =>
    typeToUse match
      case "String" =>
        assertEquals(result, this.resultString.getData.get)
      case "Map[String, String]" =>
        val res = Json.parse(result).validate[Map[String, String]]
        res match {
          case JsSuccess(resMap, _) =>
            assertEquals(resMap, this.resultMap.getData.get)
          case JsError(errors) =>
            println(errors)
        }

  Then("""^They will aggregate partial results obtaining (.*)$""") : (aggregate: String) =>
    typeToUse match
      case "String" =>
        assertEquals(aggregate, this.resultString1.aggregate(this.resultString2).getData.get)
        // assertEquals(aggregate, this.resultString2.aggregate(this.resultString1).getData.get)
      case "Map[String, String]" =>
        val res = Json.parse(aggregate).validate[Map[String, String]]
        res match {
          case JsSuccess(resMap, _) =>
            assertEquals(resMap, this.resultMap1.aggregate(this.resultMap2).getData.get)
            // assertEquals(resMap, this.resultMap2.aggregate(this.resultMap1).getData.get)
          case JsError(errors) =>
            println(errors)
        }