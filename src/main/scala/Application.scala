package org.unibo.scooby

import Application.scooby
import dsl.{ScoobyApplication, ScoobyEmbeddable}

import scala.concurrent.duration.DurationInt

object Application extends ScoobyApplication:

  scooby:
    config:
      network:
        Timeout is 5.seconds
        MaxRequests is 5
      option:
        MaxDepth is 1
        MaxLinks is 100

    crawl:
      url:
        "https://www.example.com"

    scrape:
      Iterable(
        document.url
      )

    exports as:
      println(results.map(_ / "ciao"))




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

