package org.unibo.scooby

import org.unibo.scooby.utility.document.html.HTMLElement

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

import Application.scooby
import dsl.ScoobyEmbeddable
import org.unibo.scooby.dsl.Scrape.haveAttribute
import org.unibo.scooby.dsl.Scrape.haveAttributeValue
import cats.syntax.group

object Application extends ScoobyEmbeddable with App:

  val app = scooby:
    config:
      network:
        Timeout is 5.seconds
        MaxRequests is 5
      option:
        MaxDepth is 0
        MaxLinks is 20

    crawl:
      url:
        "https://www.iana.org/help/example-domains"
      policy:
        links
    scrape:
      elements that (haveAttribute("href") and dont(
        haveAttributeValue("href", "/domains/reserved") or
        haveAttributeValue("href", "/about")
      ))

    exports:
      Batch:
        strategy:
          println(results get(_.attr("href")))
          
      Streaming:
        println(results)

  val result = Await.result(app.run(), 10.seconds)
  println(result)
