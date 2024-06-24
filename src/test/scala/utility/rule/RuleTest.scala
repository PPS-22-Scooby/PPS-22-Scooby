package org.unibo.scooby
package utility.rule

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers.*
import Rule.given

class RuleTest extends AnyFlatSpec:

  "A base rule" should "execute normally" in :
    val rule = (x: Int) => x + 1
    assert((rule executeOn 1) == 2)
    assert((rule executeOn 2) == 3)

  it should "be composable with >> operator" in :
    val rule1 = (x: Int) => x + 1
    val rule2 = (x: Int) => x * 2
    val composedRule = rule1 >> rule2

    assert((composedRule executeOn 1) == 4)
    assert((composedRule executeOn 2) == 6)

  "A policy rule" should "execute policy" in :
    val rule: Policy[Int] = (x: Int) => x > 0
    assert(rule executeOn 1)
    assert(!(rule executeOn -1))
  
  it should "be composable with and operator" in :
    val rule1 = (x: Int) => x > 0
    val rule2 = (x: Int) => x < 2
    val composedRule = rule1 and rule2
    
    assert(composedRule.executeOn(1))
    assert(!composedRule.executeOn(2))

  it should "be composable with or operator" in :
    val rule1 = (x: Int) => x > 0
    val rule2 = (x: Int) => x < 0
    val composedRule = rule1 or rule2

    assert(composedRule.executeOn(1))
    assert(composedRule.executeOn(-1))
    assert(!composedRule.executeOn(0))

  
  it should "fail compose functions with incompatible types" in:
    val rule1 = (x: Int) => x + 1
    val rule2 = (x: String) => x.length
    assertDoesNotCompile("val composedRule = rule1 >> rule2")
  
