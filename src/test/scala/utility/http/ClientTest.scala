package utility.http

import org.unibo.scooby.utility.ScalaTestWithMockServer
import org.unibo.scooby.utility.http.{ClientConfiguration, Request, URL}
import org.unibo.scooby.utility.http.Clients.SimpleHttpClient

class ClientTest extends ScalaTestWithMockServer:

  "A Client" should "block exceeding number of requests" in :
    val url = URL("http://localhost:8080/")
    val client: SimpleHttpClient = SimpleHttpClient(ClientConfiguration.default.copy(maxRequests = 2))
    val request = Request.builder.at(url).build.fold(err => fail(err.message), identity)
    val res1 = request.send(client)
    val res2 = request.send(client)
    val res3 = request.send(client)

    res1.isRight shouldBe true
    res2.isRight shouldBe true
    res3 match
      case Left(err) => err.message shouldBe "Reached the maximum amount of requests"
      case Right(_) => fail("Res3 is not a Left")

