package org.unibo.scooby
package utility.http.api

import utility.http.{Header, HttpError}
import utility.http.HttpMethod.{GET, POST}
import utility.http.Request.RequestBuilder

/**
 * Facade utility object to make HTTP calls faster and in a more readable way
 */
object Calls:
  import utility.http.{Client, Deserializer, HttpMethod, Request}

  private type RequestContext = RequestBuilderMutable ?=> Unit

  private def buildUntouched(using RequestBuilderMutable): RequestBuilder = summon[RequestBuilderMutable].builder

  def GET[T, R: Client](url: String): PartialCall[T, R] =
    new PartialCall[T, R](url, HttpMethod.GET)

  def POST[T, R: Client](url: String): PartialCall[T, R] =
    new PartialCall[T, R](url, HttpMethod.POST)

  def Body(init: => String)(using mutableBuilder: RequestBuilderMutable): Unit =
    val mutableBuilder = summon[RequestBuilderMutable]
    mutableBuilder.builder = mutableBuilder.builder.body(init)

  def Headers(init: => Seq[Header])(using mutableBuilder: RequestBuilderMutable): Unit =
    mutableBuilder.builder = mutableBuilder.builder.headers(init*)

  class PartialCall[T, R: Client](val url: String, val method: HttpMethod):
    infix def sending(init: RequestContext)(using
      deserializer: Deserializer[R, T]
    ): Either[HttpError, T] =
      given mutableBuilder: RequestBuilderMutable = RequestBuilderMutable(Request.builder.at(url).method(method))
      init
      mutableBuilder.builder.build.flatMap {
        _.send[R](summon[Client[R]]).map(deserializer.deserialize)
      }.left.map(HttpError(_))

  given call2send[T, R: Client](using Deserializer[R, T]): Conversion[PartialCall[T, R], Either[HttpError, T]] with
    override def apply(partialCall: PartialCall[T, R]): Either[HttpError, T] =
      partialCall sending buildUntouched

  sealed case class RequestBuilderMutable(var builder: RequestBuilder)
