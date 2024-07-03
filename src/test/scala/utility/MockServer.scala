package org.unibo.scooby
package utility

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.scaladsl.AskPattern.*
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import io.cucumber.scala.{EN, ScalaDsl}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.matchers.should.Matchers.{be, should}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

trait ScalaTestWithMockServer extends AnyFlatSpec, Matchers, BeforeAndAfterAll:
  implicit val timeout: Timeout = 30.seconds

  val testKit: ActorTestKit = ActorTestKit()

  implicit val system: ActorSystem[Nothing] = testKit.system
  val webServerSystem: ActorSystem[MockServer.Command] = ActorSystem(MockServer(), "WebServerSystem")

  override def beforeAll(): Unit =
    val startFuture = webServerSystem.ask[MockServer.Command](ref => MockServer.Start(ref))(timeout, system.scheduler)
    val result = Await.result(startFuture, timeout.duration)
    result shouldBe MockServer.ServerStarted

  override def afterAll(): Unit =
    webServerSystem ! MockServer.Stop
    testKit.shutdownTestKit()


trait CucumberTestWithMockServer extends ScalaDsl with EN:
  implicit val timeout: Timeout = 30.seconds

  val testKit: ActorTestKit = ActorTestKit()

  implicit val system: ActorSystem[Nothing] = testKit.system
  val webServerSystem: ActorSystem[MockServer.Command] = ActorSystem(MockServer(), "WebServerSystem")


  BeforeAll {
    val startFuture = webServerSystem.ask[MockServer.Command](ref => MockServer.Start(ref))(timeout, system.scheduler)
    val result = Await.result(startFuture, timeout.duration)
    result should be(MockServer.ServerStarted)
  }

  AfterAll {
    webServerSystem ! MockServer.Stop
    testKit.shutdownTestKit()
  }

object MockServer:

  sealed trait Command
  sealed case class Start(replyTo: ActorRef[Command]) extends Command
  case object Stop extends Command
  case object ServerStarted extends Command

  def apply(): Behavior[Command] = Behaviors.setup { context =>
    implicit val system: ActorSystem[_] = context.system
    implicit val executionContext: ExecutionContextExecutor = system.executionContext

    val route: Route =
      pathEndOrSingleSlash {
        get {
          complete(
            HttpResponse(
              entity = HttpEntity(
                ContentTypes.`text/html(UTF-8)`,
                """<html>
                  |<head><title>Simple Akka HTTP Server</title></head>
                  |<body><a href="https://www.fortest.it">Test Link</a></body>
                  |</html>""".stripMargin
              ),
              status = StatusCodes.OK
            )
          )
        }
      }

    val echo: Route =
      path("echo") {
        post {
          headerValueByName("check-header") { headerVal =>
            if (headerVal == "ok")
              extractRequest { request =>
                val headers = request.headers
                complete {
                  HttpResponse(
                    status = StatusCodes.OK,
                    entity = HttpEntity(
                      ContentTypes.`application/json`,
                      request.entity.dataBytes
                    ),

                  )
                }
              }
            else
              complete((StatusCodes.BadRequest, "The MyHeader value is invalid."))
          }
        }
      }

    val json: Route =
      path("json") {
        post {
          complete(
            HttpResponse(
              entity = HttpEntity(
                ContentTypes.`application/json`,
                "{}"
              ),
              status = StatusCodes.OK
            )
          )
        }
      }

    val notFound: Route =  extractRequest { request =>
        complete(
          HttpResponse(
            entity = HttpEntity(
              ContentTypes.`text/html(UTF-8)`,
              "<html><h1>Not Found</h1></html>"
            ),
            status = StatusCodes.NotFound
          )
        )
      }


    def running(bindingFuture: Future[Http.ServerBinding]): Behavior[Command] =
      Behaviors.receiveMessage {
        case Start(_) =>
          context.log.info("Server is already running")
          Behaviors.same

        case Stop =>
          val log = context.log
          bindingFuture.flatMap(_.unbind()).onComplete {
            case Success(_) =>
              log.info("Server stopped")
              system.terminate()
            case Failure(ex) =>
              log.error("Failed to unbind server", ex)
              system.terminate()
          }
          Behaviors.stopped
      }

    Behaviors.receiveMessage {
      case Start(replyTo) =>
        val bindingFuture = Http().newServerAt("localhost", 8080).bind(route ~ json ~ echo ~ notFound)
        val log = context.log
        bindingFuture.onComplete {
          case Success(_) =>
            log.info("Server started at http://localhost:8080/")
            replyTo ! ServerStarted  // Invia direttamente il ServerStarted
          case Failure(ex) =>
            log.error("Failed to bind HTTP endpoint, terminating system", ex)
            system.terminate()
        }
        running(bindingFuture)

      case Stop =>
        context.log.info("Server is not running")
        Behaviors.same
    }
  }
