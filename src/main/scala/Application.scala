package org.unibo.scooby

import utility.document.html.HTMLElement

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import Application.scooby
import dsl.ScoobyEmbeddable

object Application extends ScoobyEmbeddable with App:

  val app = scooby:
    config:
      network:
        Timeout is 5.seconds
        MaxRequests is 5
      option:
        MaxDepth is 2
        MaxLinks is 20

    crawl:
      url:
        "https://www.example.com"
      policy:
        links
    scrape:
      document.getElementByClass("navigation")

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
