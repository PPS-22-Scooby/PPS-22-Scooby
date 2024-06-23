package org.unibo.scooby
package utility.rule

import org.scalatest.flatspec.AnyFlatSpec

import scala.language.postfixOps
import Rule.*
import org.scalatest.matchers.should.Matchers.*

class RuleTest extends AnyFlatSpec:

  "The Rule object" should "execute normally" in :
    val rule = Rule((x: Int) => x + 1)
    assert(rule.executeOn(1) == 2)
    assert(rule.executeOn(2) == 3)

  it should "be composable with >> operator" in :
    val rule1 = Rule((x: Int) => x + 1)
    val rule2 = Rule((x: Int) => x * 2)
    val composedRule = rule1 >> rule2

    assert(composedRule.executeOn(1) == 4)
    assert(composedRule.executeOn(2) == 6)

  it should "be implicitly converted to rule" in:
    val rule: Rule[Int, Int] = ((x: Int) => x + 1).rule
    assert(rule.executeOn(1) == 2)
    assert(rule.executeOn(2) == 3)

  it should "fail compose functions with incompatible types" in:
    val rule1 = Rule((x: Int) => x + 1)
    val rule2 = Rule((x: String) => x.length)

    assertDoesNotCompile("val composedRule = rule1 >> rule2")

  
