package org.unibo.scooby
package utility.rule

import org.scalatest.flatspec.AnyFlatSpec

import scala.language.postfixOps
import Rule.*
import org.scalatest.matchers.should.Matchers.*

class RuleTest extends AnyFlatSpec:

  "Calling identity on boolean value" should "return a policy rule" in :
    identity(true) shouldBe a [PolicyRule[Any, Boolean]]

  it should "return a filter rule" in :
    identity(List("a", "b")) shouldBe a [FilterRule[Any, List[_]]]