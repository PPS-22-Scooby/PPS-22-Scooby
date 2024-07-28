package org.unibo.scooby

import org.unibo.scooby.Application.scooby
import org.unibo.scooby.dsl.ScoobyApplication

import scala.concurrent.duration.DurationInt

object Application extends ScoobyApplication:

  scooby:
    config:
      network:
        Timeout is 9.seconds
        MaxRequests is 5
        headers:
          "User-Agent" to "Scooby/1.0-alpha (https://github.com/PPS-22-Scooby/PPS-22-Scooby)"
      options:
        MaxDepth is 2
        MaxLinks is 20

    crawl:
      url:
        "https://www.example.com"
      policy:
        hyperlinks not external
    scrape:
      elements
    exports:
      batch:
        strategy:
          results get(el => (el.tag, el.text)) output:
            toConsole withFormat json

        aggregate:
          _ ++ _
