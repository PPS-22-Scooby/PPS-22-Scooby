package org.unibo.scooby

import dsl.ScoobyEmbeddable
import dsl.DSL.*
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

object Application extends ScoobyEmbeddable with App:
  import scala.concurrent.ExecutionContext.Implicits.global

  val app = scooby:
    config:
      network:
        Timeout is 100.seconds
        MaxRequests is 5
      option:
        MaxDepth is 3
        MaxLinks is 100

  val future = app.run()
  
  future.onComplete:
    case Failure(exception) => println(exception)
    case Success(value) => println(value)
  
  