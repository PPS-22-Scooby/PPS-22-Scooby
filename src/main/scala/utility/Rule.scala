package org.unibo.scooby
package utility

enum Rule[-A, B]:
  case FilterRule(f: A => B)
  case PolicyRule(f: A => Boolean)

  def identity(value: B): Rule[A, B] = FilterRule(_ => value)

