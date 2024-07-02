package org.unibo.scooby
package utility.http

import org.scalatest.{BeforeAndAfterAll, stats}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import utility.http.Clients.SimpleHttpClient
import utility.http.Request.RequestBuilder

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorSystem
import akka.util.Timeout
import io.cucumber.scala.{EN, ScalaDsl}
import org.scalatest.Assertions.*
import org.unibo.scooby.utility.MockServer
import akka.actor.typed.scaladsl.AskPattern.*

import scala.concurrent.duration.*
import scala.concurrent.Await


object BasicUsageStepDefinitions extends ScalaDsl with EN:

  implicit val timeout: Timeout = 30.seconds

  var request: RequestBuilder = Request.builder
  var response: Either[String, Response] = Left("Empty response")
  val httpClient: SimpleHttpClient = SimpleHttpClient()
  val testKit: ActorTestKit = ActorTestKit()

  implicit val system: ActorSystem[Nothing] = testKit.system
  val webServerSystem: ActorSystem[MockServer.Command] = ActorSystem(MockServer(), "WebServerSystem")


  BeforeAll {
    val startFuture = webServerSystem.ask[MockServer.Command](ref => MockServer.Start(ref))(timeout, system.scheduler)
    val result = Await.result(startFuture, timeout.duration)
    assert(result === MockServer.ServerStarted)
  }

  AfterAll {
    webServerSystem ! MockServer.Stop
    testKit.shutdownTestKit()
  }

  Given("""a simple {string} request"""): (requestType: String) =>
    request = request.method(HttpMethod.valueOf(requestType))

  Given("""a URL {string}"""): (url: String) =>
    request = request.at(url)


  When("""i make the HTTP call"""): () =>
    response = request.build match
      case Left(message: String) => fail("Invalid URL")
      case Right(request: Request) => request.send(httpClient)


  Then("""the returned content should be not empty"""): () =>
    response.fold(message => fail(message), _ => null)
    assert(response.isRight)
    assert(response.fold(_ => false, _.body.nonEmpty))

  Then("""it should return an error"""): () =>
    assert(response.isLeft)


  Then("""the status code should be {int} and the header content-type {string}"""):
    (statusCode: Int, contentType: String) =>
    assert(response.fold(message => fail(message), _.status.code) == statusCode)
    assert(response.fold(message => fail(message), _.headers("content-type")) === contentType)

