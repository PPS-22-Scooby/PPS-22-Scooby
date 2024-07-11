package org.unibo.scooby
package core.scraper.result

import io.cucumber.scala.{EN, ScalaDsl}
import org.jsoup.Jsoup
import org.junit.Assert.{assertEquals, assertNotEquals}
import play.api.libs.json.*

import scala.compiletime.uninitialized
import scala.jdk.CollectionConverters.*
import core.scraper.{Aggregator, DataResult, Result}

import org.scalatest.Entry

class StepDefinitions extends ScalaDsl with EN:

  private var typeToUse: String = uninitialized
  private var document: String = uninitialized
  private var resultMap: DataResult[(String, String)] = uninitialized
  private var entry: (String, String) = uninitialized
  private var resultString: DataResult[String] = uninitialized
  private var resultMap1: DataResult[(String, String)] = uninitialized
  private var resultMap2: DataResult[(String, String)] = uninitialized
  private var resultString1: DataResult[String] = uninitialized
  private var resultString2: DataResult[String] = uninitialized
  private val nonMatchPattern: String = "non_match"

  trait Scraper[T]:
    def scrape(doc: String): T

  Given("""^I have a (.*) result of (.*) type$""") : (part_res: String, run_type: String) =>
    typeToUse = run_type

    typeToUse match
      case "String" =>
        resultString = Result.fromData(part_res)
      case "Map[String, String]" =>
        val res = Json.parse(part_res).validate[Map[String, String]]
        res match
          case JsSuccess(resMap, _) =>
            resultMap = Result(resMap)
          case JsError(errors) =>
            println(errors)

  Given("""^I have Scrapers that elaborate documents with different filtering policies which generates different data (.*)$""") : (run_type: String) =>
    typeToUse = run_type

  Given("""^I have 2 Scrapers that elaborate documents with the same filtering policies of (.*), which works on different documents$""") : (run_type: String) =>
    typeToUse = run_type

  Given("""I have an empty result""") : () =>
    resultMap = Result.empty[(String, String)]

  Given("""I have a non empty result""") : () =>
    resultMap = Result.fromData(("a", "testA"))

  And("""^I have (.*) as document$""") : (doc: String) =>
    document = doc

  When("""^I batch a new (.*) entry$""") : (add_data: String) =>
    typeToUse match
      case "String" =>
        resultString = resultString.updateStream(add_data)
      case "Map[String, String]" =>
        val res = Json.parse(add_data).validate[Map[String, String]]
        res match
          case JsSuccess(resMap, _) =>
            resultMap = resultMap.updateStream(resMap.toSeq.head)
          case JsError(errors) =>
            println(errors)

  When("""^I batch new (.*) entries$""") : (add_data: String) =>
    typeToUse match
      case "String" =>
        val res = Json.parse(add_data).validate[Seq[String]]
        res match
          case JsSuccess(resList, _) =>
            resultString = resultString.updateBatch(resList)
          case JsError(errors) =>
            println(errors)
      case "Map[String, String]" =>
        val res = Json.parse(add_data).validate[Map[String, String]]
        res match
          case JsSuccess(resMap, _) =>
            resultMap = resultMap.updateBatch(resMap)
          case JsError(errors) =>
            println(errors)

  When("""^The scraper starts filtering the document, obtaining data to aggregate$""") : () =>
    typeToUse match
      case "String" =>
        val scraper = new Scraper[String]:
          override def scrape(doc: String): String =
            val document = Jsoup.parse(doc)
            Map(
              "a" -> document
                .getElementsByTag("a")
                .eachText()
                .asScala.toList
                .filterNot(_.contains(nonMatchPattern))
                .map("<a>".concat(_).concat("</a>"))
                .mkString(""),
              "div" -> document
                .getElementsByTag("div")
                .eachText()
                .asScala.toList
                .filterNot(_.contains(nonMatchPattern))
                .map("<div>".concat(_).concat("</div>"))
                .mkString(""),
            ).values.mkString("")
        resultString = Result.fromData(scraper.scrape(this.document))

      case "Map[String, String]" =>
        val scraper = new Scraper[Map[String, String]]:
          override def scrape(doc: String): Map[String, String] =
            val document = Jsoup.parse(doc)
            Map(
              "a" -> document
                .getElementsByTag("a")
                .eachText()
                .asScala.toList
                .filterNot(_.contains(nonMatchPattern))
                .map("<a>".concat(_).concat("</a>"))
                .mkString(""),
              "div" -> document
                .getElementsByTag("div")
                .eachText()
                .asScala.toList
                .filterNot(_.contains(nonMatchPattern))
                .map("<div>".concat(_).concat("</div>"))
                .mkString(""),
            )
        resultMap = Result(scraper.scrape(this.document))

  When("""^The scrapers finished, generated different (.*) as result$""") : (result: String) =>
    typeToUse match
      case "String" =>
        val res = Json.parse(result).validate[Seq[String]]
        res match
          case JsSuccess(resList, _) =>
            resultString1 = Result.fromData(resList(0))
            resultString2 = Result.fromData(resList(1))
          case JsError(errors) =>
            println(errors)
      case "Map[String, String]" =>
        val res = Json.parse(result).validate[Seq[Map[String, String]]]
        res match
          case JsSuccess(resMap, _) =>
            resultMap1 = Result(resMap(0))
            resultMap2 = Result(resMap(1))
          case JsError(errors) =>
            println(errors)

  When("""The scraper obtain a new entry and update the result""") : () =>
    entry = ("b", "testB")

  Then("""^The result should be (.*) one$""") : (result: String) =>
    typeToUse match
      case "String" =>
        val res = Json.parse(result).validate[List[String]]
        res match
          case JsSuccess(resList, _) =>
            assertEquals(resList, resultString.data)
          case JsError(errors) =>
            println(errors)
      case "Map[String, String]" =>
        val res = Json.parse(result).validate[Map[String, String]]
        res match
          case JsSuccess(resMap, _) =>
            assertEquals(resMap, resultMap.data)
          case JsError(errors) =>
            println(errors)

  Then("""^It will obtain (.*) as result$""") : (result: String) =>
    typeToUse match
      case "String" =>
        val res = Json.parse(result).validate[List[String]]
        res match
          case JsSuccess(resList, _) =>
            assertEquals(resList, this.resultString.data)
          case JsError(errors) =>
            println(errors)
      case "Map[String, String]" =>
        val res = Json.parse(result).validate[Map[String, String]]
        res match
          case JsSuccess(resMap, _) =>
            assertEquals(resMap, this.resultMap.data)
          case JsError(errors) =>
            println(errors)

  Then("""^They will aggregate partial results obtaining (.*) as result$""") : (aggregate: String) =>
    typeToUse match
      case "String" =>
        val res = Json.parse(aggregate).validate[List[String]]
        res match
          case JsSuccess(resList, _) =>
            assertEquals(resList, this.resultString1.aggregate(this.resultString2).data)
          case JsError(errors) =>
            println(errors)
      case "Map[String, String]" =>
        val res = Json.parse(aggregate).validate[Map[String, String]]
        res match
          case JsSuccess(resMap, _) =>
            assertEquals(resMap, this.resultMap1.aggregate(this.resultMap2).data)
          case JsError(errors) =>
            println(errors)

  Then("""The result should be updated with the new entry""") : () =>
    val res = resultMap.updateStream(entry)
    assertNotEquals(resultMap, res)
