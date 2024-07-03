package org.unibo.scooby
package utility.http

import utility.ScalaTestWithMockServer
import utility.http.Clients.SimpleHttpClient
import utility.http.api.Calls.*

import org.scalatest.flatspec.AnyFlatSpec

class ApiTest extends ScalaTestWithMockServer:

  val validUrl = "http://localhost:8080"

  "A simple GET request made with the API" should "be equivalent to the more verbose library version" in:
    given client: SimpleHttpClient = new SimpleHttpClient
    val verboseResponseBody: Either[String, String] =
      Request.builder.at(validUrl).build.flatMap{_.send(client).map(_.body.getOrElse(""))}

    val apiResponseBody: Either[String, String] = GET(validUrl)
    verboseResponseBody.isRight should be(true)
    apiResponseBody.isRight should be(true)
    verboseResponseBody should be(apiResponseBody)


  "A custom POST request made with the API" should "be equivalent to the more verbose library version" in :
    given client: SimpleHttpClient = new SimpleHttpClient
    val body = """{ "example" : [1,2,3]}"""
    val echoUrl = validUrl + "/echo"
    val headers = Map("check-header" -> "ok")
    val verboseResponse: Either[String, Response] =
      Request.builder
        .at(echoUrl)
        .post()
        .headers(headers.toSeq*)
        .body(body)
        .build.flatMap(_.send(client))

    val apiResponseBody: Either[String, String] = POST(echoUrl)
    verboseResponse.fold(message => fail(message), response =>
      response.status should be(HttpStatus.OK)
      response.headers("content-type") should be("application/json")
      response.body should be(Some(body))
      response.body should be(apiResponseBody.getOrElse(fail("apiResponseBody was a Left")))
    )
