package org.unibo.scooby

import utility.document.html.HTMLElement

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import Application.scooby
import dsl.ScoobyEmbeddable

import org.unibo.scooby.dsl.Scrape.{element, haveAttribute, haveAttributeValue, rule}
import cats.syntax.group

object Application extends ScoobyEmbeddable with App:

  val app = scooby:
    config:
      network:
        Timeout is 9.seconds
        MaxRequests is 5
        headers:
          "Authorization" to "prova"
          "Agent" to "gr"
      option:
        MaxDepth is 2
        MaxLinks is 20

    crawl:
      url:
        "https://www.iana.org/help/example-domains"
      policy:
        links
    scrape:
      elements that :
        haveAttribute("href") and dont:
          haveAttributeValue("href", "/domains/reserved") or
          haveAttributeValue("href", "/about")
        .and:
          rule { element.tag == "a" } and rule { element.tag == "div" }
    exports:
      Batch:
        strategy:
          results get tag output:
            ToConsole withFormat Text
            ToFile("prova") withFormat Text

        aggregate:
          _ ++ _
          
      Streaming:
        results.groupBy(_.tag).view.mapValues(_.size).toMap output:
          ToConsole withFormat Text

  val result = Await.result(app.run(), 10.seconds)
  println(result)
