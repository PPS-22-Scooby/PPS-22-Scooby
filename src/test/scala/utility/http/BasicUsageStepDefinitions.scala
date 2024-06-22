package org.unibo.scooby
package utility.http

import utility.http.Clients.SimpleHttpClient
import utility.http.Request.RequestBuilder

import io.cucumber.scala.{EN, ScalaDsl}
import org.scalatest.Assertions.*

import scala.util.Try


class BasicUsageStepDefinitions extends ScalaDsl with EN:

  var request: RequestBuilder = Request.builder
  var response: Try[Response] = Try { Response.empty }
  given httpClient: SimpleHttpClient = SimpleHttpClient()

  Given("""a simple {string} request"""): (requestType: String) =>
    request = request.method(HttpMethod.valueOf(requestType))

  Given("""a URL {string}"""): (url: String) =>
    request = request.at(url)


  When("""i make the HTTP call"""): () =>
    response = request.send


  Then("""the returned content should be not empty"""): () =>
    assert(response.get.body.nonEmpty)

  Then("""it should return an error"""): () =>
    assert(response.isFailure)


  Then("""the status code should be {int} and the header content-type {string}"""):
    (statusCode: Int, contentType: String) =>
    assert(response.get.status.code === statusCode)
    assert(response.get.headers("content-type") === contentType)

