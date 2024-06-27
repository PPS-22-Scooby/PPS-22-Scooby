package org.unibo.scooby
package utility.http

import io.cucumber.scala.{EN, ScalaDsl}
import org.scalatest.Assertions.*

import scala.util.Try

class UrlManagementStepDefinitions extends ScalaDsl with EN:

  var inputString: String = ""
  var inputUrl1: Either[String, URL] = URL("")
  var inputUrl2: Either[String, URL] = URL("")
  var compareResult: Boolean = false

  Given("""the string {string}"""): (string: String) =>
    inputString = string

  Given("""two URLs {string} and {string}"""): (string: String, string2: String) =>
    inputUrl1 = URL(string).orElse { fail("Illegal URL provided") }
    inputUrl2 = URL(string2).orElse { fail("Illegal URL provided") }

  Given("""two URLs {string} and a string {string}"""): (url: String, stringUrl: String) =>
    inputUrl1 = URL(url).orElse { fail("Illegal URL provided") }
    inputString = stringUrl


  Given("""the URL {string}"""): (string: String) =>
    inputUrl1 = URL(string).orElse { fail("Illegal URL provided") }


  When("""i convert it to URL"""): () =>
    inputUrl1 = URL(inputString)

  When("""i append them"""): () =>
    inputUrl1 = (inputUrl1, inputUrl2) match
      case (u1, u2) if u1.isLeft || u2.isLeft  => Left("Illegal URL")
      case (Right(url1), Right(url2)) => Right(url1 / url2)

  When("""i append the string to the url"""): () =>
    inputUrl1 = inputUrl1 match
      case Left(_) => Left("Illegal URL")
      case Right(u) => Right(u / inputString)

  When("""i get the domain"""): () =>
    inputString = inputUrl1.getOrElse{ fail("Illegal URL") }.domain

  When("""i go to the parent"""): () =>
    inputUrl1 = inputUrl1 match
      case Left(_) => Left("Illegal URL")
      case Right(u) => Right(u.parent)

  When("""i compare them"""): () =>
    compareResult = inputUrl1.getOrElse{ fail("Illegal URL") } < inputUrl2.getOrElse{ fail("Illegal URL") }


  Then("""it should return a valid URL"""): () =>
    assert(inputUrl1.isRight)

  Then("""it should result in a Malformed URL error"""): () =>
    // Write code here that turns the phrase above into concrete actions
    assert(inputUrl1.isLeft)

  Then("""it should return the URL {string}"""): (url: String) =>
    val result = inputUrl1.getOrElse{ fail("Illegal URL") }
    assert(result.toString == url)

  Then("""it should return the domain {string}"""): (domain: String) =>
    assert(inputString == domain)

  Then("""the first should be lower than the second"""): () =>
    assert(compareResult)

