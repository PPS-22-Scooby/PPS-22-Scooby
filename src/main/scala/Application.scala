package org.unibo.scooby

import dsl.Config.CrawlerGlobalConfiguration.option
import dsl.Config.NetworkConfiguration.network
import dsl.ScoobyEmbeddable
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

class Application extends ScoobyEmbeddable:
  import scala.concurrent.ExecutionContext.Implicits.global

  val app = scooby:
    config:
      network:
        Timeout -> 5.seconds
      option:
        ???

  val future = app.run()
  
  future.onComplete:
    case Failure(exception) => println(exception)
    case Success(value) => println(value)
  
  