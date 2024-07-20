package org.unibo.scooby

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

import Application.scooby
import dsl.ScoobyEmbeddable
import org.unibo.scooby.core.exporter.Exporter.batch
import org.unibo.scooby.dsl.Export.strategy
import org.unibo.scooby.utility.document.html.HTMLElement
import javax.swing.text.html.HTML

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


    exports as:
      stream:
        println(results[HTMLElement].groupBy(_.tag == "div").mapValues(_.size).toMap)

  val result = Await.result(app.run(), 10.seconds)
  println(result)
