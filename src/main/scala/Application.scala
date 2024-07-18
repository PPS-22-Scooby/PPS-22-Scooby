package org.unibo.scooby

import Application.scooby
import dsl.ScoobyApplication

import scala.concurrent.duration.DurationInt

object Application extends ScoobyApplication:

  scooby:
    config:
      network:
        Timeout is 100.seconds
        MaxRequests is 5
      option:
        MaxDepth is 3
        MaxLinks is 100

    crawl:
      url:
        "https://docs.scala-lang.org/overviews/core/custom-collections.html"
      policy:
        links.filter(_.toString.contains("/community/"))


