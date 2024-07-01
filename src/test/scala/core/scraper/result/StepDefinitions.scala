package org.unibo.scooby
package core.scraper.result

import core.scraper.{Result, ResultImpl, Aggregator}

import io.cucumber.scala.{EN, ScalaDsl}
import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import play.api.libs.json.*

import scala.jdk.CollectionConverters.*

class StepDefinitions extends ScalaDsl with EN:

  private var typeToUse: String = _
  private var document: String = _
  private var resultMap: Result[(String, String)] = _
  private var resultString: Result[String] = _
  private var resultMap1: Result[(String, String)] = _
  private var resultMap2: Result[(String, String)] = _
  private var resultString1: Result[String] = _
  private var resultString2: Result[String] = _
  private val nonMatchPattern: String = "non_match"

  trait Scraper[T] {
    def scrape(doc: String): T
  }

  Given("""^I have a (.*) (.*)$""") : (run_type: String, part_res: String) =>
    typeToUse = run_type

    print(typeToUse)
    typeToUse match
      case "String" =>
        resultString = ResultImpl(Iterable(part_res))
      case "Map[String, String]" =>
        val res = Json.parse(part_res).validate[Map[String, String]]
        res match {
          case JsSuccess(resMap, _) =>
            resultMap = ResultImpl(resMap)
          case JsError(errors) =>
            println(errors)
        }

  Given("""^I have Scrapers that elaborate documents with different filtering policies which generates different data (.*)$""") : (run_type: String) =>
    typeToUse = run_type

  Given("""^I have 2 Scrapers that elaborate documents with the same filtering policies of (.*), which works on different documents$""") : (run_type: String) =>
    typeToUse = run_type

  And("""^There are different (.*)$""") : (doc: String) =>
    document = doc

  When("""^I batch a new entry (.*)$""") : (add_data: String) =>
    typeToUse match
      case "String" =>
        resultString = resultString.updateStream(add_data)
      case "Map[String, String]" =>
        val res = Json.parse(add_data).validate[Map[String, String]]
        res match {
          case JsSuccess(resMap, _) =>
            resultMap = resultMap.updateStream(resMap.toSeq.head)
          case JsError(errors) =>
            println(errors)
        }

  When("""^I batch new entries (.*)$""") : (add_data: String) =>
    typeToUse match
      case "String" =>
        val res = Json.parse(add_data).validate[List[String]]
        res match {
          case JsSuccess(resList, _) =>
            resultString = resultString.updateBatch(resList)
          case JsError(errors) =>
            println(errors)
        }
      case "Map[String, String]" =>
        val res = Json.parse(add_data).validate[Map[String, String]]
        res match {
          case JsSuccess(resMap, _) =>
            resultMap = resultMap.updateBatch(resMap)
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
        resultString = ResultImpl(Iterable(scraper.scrape(this.document)))

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
        resultMap = ResultImpl(scraper.scrape(this.document))

  When("""^The scrapers finished, generated different (.*)$"""): (result: String) =>
    typeToUse match
      case "String" =>
        val res = Json.parse(result).validate[List[String]]
        res match {
          case JsSuccess(resList, _) =>
            resultString1 = ResultImpl(Iterable(resList(0)))
            resultString2 = ResultImpl(Iterable(resList(1)))
          case JsError(errors) =>
            println(errors)
        }
      case "Map[String, String]" =>
        val res = Json.parse(result).validate[List[Map[String, String]]]
        res match {
          case JsSuccess(resMap, _) =>
            resultMap1 = ResultImpl(resMap(0))
            resultMap2 = ResultImpl(resMap(1))
          case JsError(errors) =>
            println(errors)
        }

  Then("""^The result should be (.*)$""") : (result: String) =>
    typeToUse match
      case "String" =>
        val res = Json.parse(result).validate[List[String]]
        res match {
          case JsSuccess(resList, _) =>
            assertEquals(resList, resultString.data)
          case JsError(errors) =>
            println(errors)
        }
      case "Map[String, String]" =>
        val res = Json.parse(result).validate[Map[String, String]]
        res match {
          case JsSuccess(resMap, _) =>
            assertEquals(resMap, resultMap.data)
          case JsError(errors) =>
            println(errors)
        }

  Then("""^It will obtain (.*)$""") : (result: String) =>
    typeToUse match
      case "String" =>
        val res = Json.parse(result).validate[List[String]]
        res match {
          case JsSuccess(resList, _) =>
            assertEquals(resList, this.resultString.data)
          case JsError(errors) =>
            println(errors)
        }
      case "Map[String, String]" =>
        val res = Json.parse(result).validate[Map[String, String]]
        res match {
          case JsSuccess(resMap, _) =>
            assertEquals(resMap, this.resultMap.data)
          case JsError(errors) =>
            println(errors)
        }

  Then("""^They will aggregate partial results obtaining (.*)$""") : (aggregate: String) =>
    typeToUse match
      case "String" =>
        val res = Json.parse(aggregate).validate[List[String]]
        res match {
          case JsSuccess(resList, _) =>
            assertEquals(resList, this.resultString1.aggregate(this.resultString2).data)
          case JsError(errors) =>
            println(errors)
        }
      case "Map[String, String]" =>
        val res = Json.parse(aggregate).validate[Map[String, String]]
        res match {
          case JsSuccess(resMap, _) =>
            assertEquals(resMap, this.resultMap1.aggregate(this.resultMap2).data)
          case JsError(errors) =>
            println(errors)
        }