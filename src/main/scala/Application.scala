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
        MaxDepth is 1
        MaxLinks is 100

    scrape:
      Iterable(
        (document.content.head, document.content.length)
      )

    exports as:
      results.foreach(println(_))


//  scrape {
//
//  } exports:

//  scrape:
//    ...
//  .exports:

//  scrape:
//    ...
//  >> exports:
//    ...

//    crawl:
//      url:
//        "https://docs.scala-lang.org/overviews/core/custom-collections.html"
//      policy:
//        links filter (_.toString.contains("/community/"))

//    scrape:
//      Iterable(s"${document.content.head}")
//
//
//      batch:
//
//      stream:




//    export:
//      batch:
//        policy: // la policy di esportazione
//        aggregate:
//
//      batch:
//        policy: // la policy di esportazione
//        aggregate:
//
//      stream:
//        policy:

