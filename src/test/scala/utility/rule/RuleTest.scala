package org.unibo.scooby
package utility.rule

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers.*
import Rule.{*, given}

class RuleTest extends AnyFlatSpec:

  "The Rule object" should "execute normally" in :
    val rule = Rule((x: Int) => x + 1)
    assert(rule.executeOn(1).contains(2))
    assert(rule.executeOn(2).contains(3))

  it should "execute policy" in :
    val rule = policy((x: Int) => x > 0)
    assert(rule.executeOn(1).contains(true))
    assert(rule.executeOn(-1).contains(false))

  it should "not execute invalid" in :
    val rule = Invalid[Int, Int]()
    assert(rule.executeOn(1).isEmpty)

  it should "be composable with >> operator" in :
    val rule1 = Rule((x: Int) => x + 1)
    val rule2 = Rule((x: Int) => x * 2)
    val composedRule = rule1 >> rule2

    assert(composedRule.executeOn(1).contains(4))
    assert(composedRule.executeOn(2).contains(6))

  it should "be composable with and operator" in :
    val rule1 = policy((x: Int) => x > 0)
    val rule2 = policy((x: Int) => x < 2)
    val composedRule = rule1 and rule2

    assert(composedRule.executeOn(1).contains(true))
    assert(composedRule.executeOn(2).contains(false))

  it should "be composable with or operator" in :
    val rule1 = policy((x: Int) => x > 0)
    val rule2 = policy((x: Int) => x < 0)
    val composedRule = rule1 or rule2

    assert(composedRule.executeOn(1).contains(true))
    assert(composedRule.executeOn(-1).contains(true))
    assert(composedRule.executeOn(0).contains(false))

  it should "be implicitly converted to rule" in:
    val rule: Rule[Int, Int] = ((x: Int) => x + 1).rule
    assert(rule.executeOn(1).contains(2))
    assert(rule.executeOn(2).contains(3))

  it should "fail compose functions with incompatible types" in:
    val rule1 = Rule((x: Int) => x + 1)
    val rule2 = Rule((x: String) => x.length)

    assertDoesNotCompile("val composedRule = rule1 >> rule2")
  
