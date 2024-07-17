package org.unibo.scooby

import dsl.ScoobyEmbeddable

import scala.util.{Failure, Success}

class Application extends ScoobyEmbeddable:
  import scala.concurrent.ExecutionContext.Implicits.global

  val app = scooby:
    ???
  val future = app.run()
  
  future.onComplete:
    case Failure(exception) => println(exception)
    case Success(value) => println(value)
  
  