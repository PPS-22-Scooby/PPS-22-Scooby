package org.unibo.scooby

import utility.document.html.HTMLElement

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import Application.scooby
import dsl.ScoobyEmbeddable
import dsl.Export.Context.ExportToContext

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

          // println(results.groupBy(_.tag).mapValues(_.size).toMap)
        aggregate:
          _ ++ _
          
      Streaming:
        results.groupBy(_.tag).mapValues(_.size).toMap outputTo:
          File("prova") asStrategy Json

  val result = Await.result(app.run(), 10.seconds)
  println(result)
