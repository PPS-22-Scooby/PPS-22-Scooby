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
    val verboseResponseBody: Either[HttpError, String] =
      Request.builder.at(validUrl).build.flatMap{_.send(client).map(_.body.getOrElse(""))}

    val apiResponseBody: Either[HttpError, String] = GET(validUrl)
    verboseResponseBody.isRight should be(true)
    apiResponseBody.isRight should be(true)
    verboseResponseBody should be(apiResponseBody)


  "A custom POST request made with the API" should "be equivalent to the more verbose library version" in :
    given client: SimpleHttpClient = new SimpleHttpClient
    val body = """{ "example" : [1,2,3]}"""
    val echoUrl = validUrl + "/echo"
    val headers = Map("check-header" -> "ok")
    val verboseResponse: Either[HttpError, Response] =
      Request.builder
        .at(echoUrl)
        .post()
        .headers(headers.toSeq*)
        .body(body)
        .build.flatMap(_.send(client))
    
    val apiResponseBody: Either[HttpError, Option[String]] =
      POST(echoUrl) sending:
        Body:
          body
        Headers:
          headers.toSeq

    verboseResponse.fold(error => fail(error.message), response =>
      response.status should be(HttpStatus.OK)
      response.headers("content-type") should be("application/json")
      response.body should be(Some(body))
      response.body should be(apiResponseBody.getOrElse(fail("apiResponseBody was a Left")))
    )

  "A GET request to an invalid URL" should "return a Left indicating the error" in:
    given client: SimpleHttpClient = new SimpleHttpClient
    val response: Either[HttpError, Response] =
      GET(":http") sending:
        Body {"examplebody"}

    response.fold(_.message, _ => fail("Should be a Left")) should be("Invalid URL was provided")
