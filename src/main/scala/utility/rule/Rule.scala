package org.unibo.scooby
package utility.rule

enum Rule[-A, B]:
  case FilterRule(f: A => B)
  case PolicyRule(f: A => Boolean)

object Rule:
  def identity[B](value: B): Rule[Any, B] = value match
    case b: Boolean => PolicyRule(_ => b)
    case _ => FilterRule(_ => value)




