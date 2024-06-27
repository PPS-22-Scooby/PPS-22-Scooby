package org.unibo.scooby
package utility.http

import utility.http.Clients.SimpleHttpClient
import utility.http.Request.RequestBuilder

import io.cucumber.scala.{EN, ScalaDsl}
import org.scalatest.Assertions.*

import scala.util.Try


class BasicUsageStepDefinitions extends ScalaDsl with EN:

  var request: RequestBuilder = Request.builder
  var response: Either[String, Response] = Left("Empty response")
  given httpClient: SimpleHttpClient = SimpleHttpClient()

  Given("""a simple {string} request"""): (requestType: String) =>
    request = request.method(HttpMethod.valueOf(requestType))

  Given("""a URL {string}"""): (url: String) =>
    request = request.at(url)


  When("""i make the HTTP call"""): () =>
    response = request.send


  Then("""the returned content should be not empty"""): () =>
    assert(response.isRight)
    assert(response.fold(_ => false, _.body.nonEmpty))

  Then("""it should return an error"""): () =>
    assert(response.isLeft)


  Then("""the status code should be {int} and the header content-type {string}"""):
    (statusCode: Int, contentType: String) =>
    assert(response.fold(_ => false, _.status.code === statusCode))
    assert(response.fold(_ => false, _.headers("content-type") === contentType))

