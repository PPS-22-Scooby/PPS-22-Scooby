package org.unibo.scooby

import dsl.Config.CrawlerGlobalConfiguration.option
import dsl.Config.NetworkConfiguration.network
import dsl.ScoobyEmbeddable
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

object Application extends ScoobyEmbeddable with App:
  import scala.concurrent.ExecutionContext.Implicits.global

  val app = scooby:
    config:
      network:
        Timeout --> 100.seconds
        MaxRequests --> 1
      option:
        ???

  val future = app.run()
  
  future.onComplete:
    case Failure(exception) => println(exception)
    case Success(value) => println(value)
  
  