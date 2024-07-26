package org.unibo.scooby

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import Application.scooby
import dsl.ScoobyEmbeddable

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
        "https://www.example.com"
      policy:
        hyperlinks not external
    scrape:
      elements that :
        haveAttribute("href") and dont:
          haveAttributeValue("href", "/domains/reserved") or
          haveAttributeValue("href", "/about")
        .and:
          followRule { element.tag == "a" } and followRule { element.tag == "div" }
    exports:
      batch:
        strategy:
          results get tag output:
            toConsole withFormat Text
            toFile("prova") withFormat Text

        aggregate:
          _ ++ _
          
//      streaming:
//        results.groupBy(_.tag).view.mapValues(_.size).toMap output:
//          toConsole withFormat Text

  val result = Await.result(app.run(), 10.seconds)
  println(result)
