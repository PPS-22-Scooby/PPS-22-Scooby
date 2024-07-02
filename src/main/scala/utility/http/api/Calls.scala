package org.unibo.scooby
package utility.http.api

import utility.http.Request.RequestBuilder


/**
 * Facade utility object to make HTTP calls faster and in a more readable way
 */
object Calls:
  import utility.http.{Client, Deserializer, HttpMethod, Request}

  import Context.*

  private def fetch[T, R: Client](using deserializer: Deserializer[R, T])
                                  (method: HttpMethod)
                                  (init: RequestContext ?=> RequestBuilder): Either[String, T] =
    val builder = Request.builder
    given context: RequestContext = new RequestContext(builder)
    init
    val client: Client[R] = summon[Client[R]]
    builder.build.flatMap { _.send[R](client).map(deserializer.deserialize)}.left.map(identity)


  private object Context:

    class RequestContext(var builder: RequestBuilder)
