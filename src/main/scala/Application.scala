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
        MaxDepth is 1
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
          results get attr("myAttr") outputTo:
            File("prova") asStrategy Text

        aggregate:
          _ ++ _
          
      Streaming:
        results.groupBy(_.tag).view.mapValues(_.size).toMap outputTo:
          Console asStrategy Json

  val result = Await.result(app.run(), 10.seconds)
  println(result)
