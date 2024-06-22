package org.unibo.scooby
package utility.http

import io.cucumber.scala.{EN, ScalaDsl}
import org.unibo.scooby.utility.http.Request.RequestBuilder
import org.scalatest.Assertions.*


class BasicUsageStepDefinitions extends ScalaDsl with EN:

  var request: RequestBuilder = Request.builder
  var response: Response = Response.empty
  given httpClient: HttpClient = HttpClient()

  Given("""a simple {string} request"""): (requestType: String) =>
    request = request.method(HttpMethod.valueOf(requestType))

  Given("""a URL {string}"""): (url: String) =>
    request = request.at(url)


  When("""i make the HTTP call"""): () =>
    response = request.send


  Then("""the returned content should be not empty"""): () =>
    assert(response.body.nonEmpty)

  Then("""it should return an error"""): () =>
    assert(response.status === HttpStatus.NOT_FOUND)


  Then("""the status code should be {int} and the header Content-Type {string}"""):
    (statusCode: Int, contentType: String) =>
    assert(response.status.code === statusCode)
    assert(response.headers("Content-Type") === contentType)

