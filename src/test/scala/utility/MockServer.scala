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
import MockServer.Command.*

trait ScalaTestWithMockServer(port: Int = 8080, routes: Route = MockServer.Routes.defaultTestingRoutes)
  extends AnyFlatSpec, Matchers, BeforeAndAfterAll:
  implicit val timeout: Timeout = 30.seconds

  val testKit: ActorTestKit = ActorTestKit()

  implicit val system: ActorSystem[Nothing] = testKit.system
  val webServerSystem: ActorSystem[MockServer.Command] = ActorSystem(MockServer(port, routes), "WebServerSystem")

  override def beforeAll(): Unit =
    val startFuture = webServerSystem.ask[MockServer.Command](ref => Start(ref))(timeout, system.scheduler)
    val result = Await.result(startFuture, timeout.duration)
    result shouldBe ServerStarted

  override def afterAll(): Unit =
    webServerSystem ! Stop
    testKit.shutdownTestKit()

trait CucumberTestWithMockServer(port: Int = 8080, routes: Route = MockServer.Routes.defaultTestingRoutes)
  extends ScalaDsl with EN:
  implicit val timeout: Timeout = 30.seconds

  val testKit: ActorTestKit = ActorTestKit()

  implicit val system: ActorSystem[Nothing] = testKit.system
  val webServerSystem: ActorSystem[MockServer.Command] = ActorSystem(MockServer(port, routes), "WebServerSystem")

  BeforeAll:
    val startFuture = webServerSystem.ask[MockServer.Command](ref => Start(ref))(timeout, system.scheduler)
    val result = Await.result(startFuture, timeout.duration)
    result should be(ServerStarted)

  AfterAll:
    webServerSystem ! Stop
    testKit.shutdownTestKit()
  

object MockServer:
  enum Command:
    case Start(replyTo: ActorRef[Command]) extends Command
    case Stop extends Command
    case ServerStarted extends Command

  object Routes:

    def defaultTestingRoutes: Route =
      val index: Route =
        pathEndOrSingleSlash:
          get:
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
      val extUrl: Route =
        path("ext-url"):
          get:
            complete(
              HttpResponse(
                entity = HttpEntity(
                  ContentTypes.`text/html(UTF-8)`,
                  """<html>
                    |<head><title>Simple Akka HTTP Server</title></head>
                    |<body>
                    | <a href="http://localhost:8080/a">Same Link</a>
                    | <a href="http://localhost:8080/b">Same Link But B</a>
                    | <a href="http://www.external-url.it"> External URL</a>
                    |</body>
                    |</html>""".stripMargin
                ),
                status = StatusCodes.OK
              )
            )

      val echo: Route =
        path("echo"):
          post:
            headerValueByName("check-header"): headerVal =>
              if (headerVal == "ok")
                extractRequest: request =>
                  request.headers
                  complete:
                    HttpResponse(
                      status = StatusCodes.OK,
                      entity = HttpEntity(
                        ContentTypes.`application/json`,
                        request.entity.dataBytes
                      )
                    ) 
              else
                complete((StatusCodes.BadRequest, "The MyHeader value is invalid."))

      val json: Route =
        path("json"):
          post:
            complete(
              HttpResponse(
                entity = HttpEntity(
                  ContentTypes.`application/json`,
                  "{}"
                ),
                status = StatusCodes.OK
              )
            )

      val robotsTxt: Route =
        path("robots.txt"):
          get:
            complete(
              HttpResponse(
                entity = HttpEntity(
                  ContentTypes.`text/plain(UTF-8)`,
                  """User-agent: *
                  |Disallow: /private/
                  |Disallow: /tmp/
                  |Allow: /""".stripMargin
                ),
                status = StatusCodes.OK
              )
            )

      val notFound: Route = extractRequest: request =>
        complete:
          HttpResponse(
            entity = HttpEntity(
              ContentTypes.`text/html(UTF-8)`,
              "<html><h1>Not Found</h1></html>"
            ),
            status = StatusCodes.NotFound
          )
      
      index ~ extUrl ~ robotsTxt ~ json ~ echo ~ notFound

  def apply(port: Int = 8080, routes: Route = Routes.defaultTestingRoutes): Behavior[Command] = 
    Behaviors.setup: context =>
      implicit val system: ActorSystem[_] = context.system
      implicit val executionContext: ExecutionContextExecutor = system.executionContext

      def running(bindingFuture: Future[Http.ServerBinding]): Behavior[Command] =
        Behaviors.receiveMessage:
          case Start(_) =>
            context.log.info("Server is already running")
            Behaviors.same

          case Stop =>
            val log = context.log
            bindingFuture.flatMap(_.unbind()).onComplete:
              case Success(_) =>
                log.info("Server stopped")
                system.terminate()
              case Failure(ex) =>
                log.error("Failed to unbind server", ex)
                system.terminate()
            
            Behaviors.stopped

          case _ => Behaviors.same
        

      Behaviors.receiveMessage:
        case Start(replyTo) =>
          val bindingFuture = Http().newServerAt("localhost", port).bind(routes)
          val log = context.log
          bindingFuture.onComplete {
            case Success(_) =>
              log.info("Server started at http://localhost:8080/")
              replyTo ! ServerStarted // sends "ServerStarted" directly
            case Failure(ex) =>
              log.error("Failed to bind HTTP endpoint, terminating system", ex)
              system.terminate()
          }
          running(bindingFuture)

        case Stop =>
          context.log.info("Server is not running")
          Behaviors.same

        case _ => Behaviors.same
      
    
