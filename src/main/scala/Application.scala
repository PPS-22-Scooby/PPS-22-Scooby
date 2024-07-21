package org.unibo.scooby

import org.unibo.scooby.utility.document.html.HTMLElement

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

import Application.scooby
import dsl.ScoobyEmbeddable
import org.unibo.scooby.dsl.Crawl.not

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
        "https://www.example.it"
      policy:
        hyperlinks not external
    scrape:
      document.getElementByClass("a-price-whole")


    exports:
      Batch:
        strategy:
          println(results.map(_.text))
        aggregate:
          _ ++ _
          
      Streaming:
        println(results)

  val result = Await.result(app.run(), 10.seconds)
  println(result)
