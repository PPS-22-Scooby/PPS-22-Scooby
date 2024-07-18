package org.unibo.scooby
package utility.http

import io.cucumber.scala.{EN, ScalaDsl}
import org.scalatest.Assertions._


class UrlManagementStepDefinitions extends ScalaDsl with EN:

  var inputString: String = ""
  var inputUrl1: URL = URL.empty
  var inputUrl2: URL = URL.empty
  var compareResult: Boolean = false

  Given("""the string {string}"""): (string: String) =>
    inputString = string

  Given("""two URLs {string} and {string}"""): (string: String, string2: String) =>
    inputUrl1 = URL(string)
    inputUrl2 = URL(string2)

  Given("""a URL {string} and a string {string}"""): (url: String, stringUrl: String) =>
    inputUrl1 = URL(url)
    inputString = stringUrl


  Given("""the URL {string}"""): (string: String) =>
    inputUrl1 = URL(string)


  When("""i convert it to URL"""): () =>
    inputUrl1 = URL(inputString)

  When("""i append them"""): () =>
    inputUrl1 = inputUrl1 / inputUrl2

  When("""i append the string to the url"""): () =>
    inputUrl1 = inputUrl1 / inputString

  When("""i get the domain"""): () =>
    inputString = inputUrl1.domain

  When("""i go to the parent"""): () =>
    inputUrl1 = inputUrl1.parent

  When("""i compare them"""): () =>
    compareResult = inputUrl1 < inputUrl2


  Then("""it should return a valid URL with depth {int}"""): (depth: Int) =>
    assert(inputUrl1.isValid)
    assert(inputUrl1.depth == depth)

  Then("""it should result in a Malformed URL error"""): () =>
    // Write code here that turns the phrase above into concrete actions
    assert(!inputUrl1.isValid)

  Then("""it should return the URL {string}"""): (url: String) =>
    val result = inputUrl1
    assert(result.toString == url)

  Then("""it should return the domain {string}"""): (domain: String) =>
    assert(inputString == domain)

  Then("""the first should be lower than the second"""): () =>
    assert(compareResult)

