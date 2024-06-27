package org.unibo.scooby
package utility.http

import org.scalatest.matchers.should
import org.scalatest.flatspec.AnyFlatSpec

class CommonTests extends AnyFlatSpec with should.Matchers:

  val exampleUrl: String = "https://www.scooby.com"

  "A Request builder" should "build the GET Request with all the fields provided" in:
    val request: Request = Request.builder.get().at(exampleUrl).build.getOrElse{ fail ("Illegal URL")}
    request.url should be(URL(exampleUrl).getOrElse(URL.empty))
    request.body should be(Option.empty)
    request.method should be(HttpMethod.GET)
    request.headers should be(Map.empty)

  "A Request builder" should "fail if the URL is not provided" in:
    Request.builder.get().build.isLeft should be(true)
    Request.builder.get().at(URL.empty).build.isLeft should be(true)


  "A Request builder" should "build the POST Request with all the fields provided" in :
    val request: Request = Request.builder.post().at(exampleUrl).build.getOrElse{ fail ("Illegal URL")}
    request.url should be(URL(exampleUrl).getOrElse(URL.empty))
    request.body should be(Option.empty)
    request.method should be(HttpMethod.POST)
    request.headers should be(Map.empty)

  "A Request builder" should "build the PUT Request with all the fields provided" in :
    val request: Request = Request.builder.put().at(exampleUrl).build.getOrElse{ fail ("Illegal URL")}
    request.url should be(URL(exampleUrl).getOrElse(URL.empty))
    request.body should be(Option.empty)
    request.method should be(HttpMethod.PUT)
    request.headers should be(Map.empty)

  "A Request builder" should "build the DELETE Request with all the fields provided" in :
    val request: Request = Request.builder.delete().at(exampleUrl).build.getOrElse{ fail ("Illegal URL")}
    request.url should be(URL(exampleUrl).getOrElse(URL.empty))
    request.body should be(Option.empty)
    request.method should be(HttpMethod.DELETE)
    request.headers should be(Map.empty)

  "A Request builder" should "build the GET Request using an already built URL" in :
    val urlEither = URL(exampleUrl)
    urlEither.isRight should be(true)
    val url = urlEither.getOrElse(URL.empty)
    val request: Request = Request.builder.at(url).build.getOrElse{ fail ("Illegal URL")}
    request.url should be(url)
    request.body should be(Option.empty)
    request.method should be(HttpMethod.GET)
    request.headers should be(Map.empty)

