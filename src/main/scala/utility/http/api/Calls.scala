package org.unibo.scooby
package utility.http.api

import utility.http.Request.RequestBuilder


/**
 * Facade utility object to make HTTP calls faster and in a more readable way
 */
object Calls:
  import utility.http.{Client, Deserializer, HttpMethod, Request}
  import Context.*

  private def fetch[T, R: Client](url: String)(using deserializer: Deserializer[R, T])
                                 (method: HttpMethod)
                                 (init: RequestContext ?=> RequestBuilder): Either[String, T] =
    val builder = Request.builder.at(url).method(method)
    given context: RequestContext = new RequestContext(builder)
    init
    builder.build.flatMap {
      _.send[R](summon[Client[R]]).map(deserializer.deserialize)
    }.left.map(identity)

  private def directBuild(using context: RequestContext): RequestBuilder = context.builder

  def GET[T, R: Client](url: String, init: RequestContext ?=> RequestBuilder = directBuild)
                       (using deserializer: Deserializer[R, T]): Either[String, T] =
    fetch(url)(HttpMethod.GET)(init)


  def POST[T, R: Client](url: String, init: RequestContext ?=> RequestBuilder = directBuild)
                        (using deserializer: Deserializer[R, T]): Either[String, T] =
    fetch(url)(HttpMethod.POST)(init)



  private object Context:

    class RequestContext(var builder: RequestBuilder)
