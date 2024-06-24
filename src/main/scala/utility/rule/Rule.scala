package org.unibo.scooby
package utility.rule

import scala.language.implicitConversions

trait Rule[A, B]:
  def f : A => B
  def executeOn(value: A): B = f(value)

sealed trait JoinRule[A, B] extends Rule[A, B]:
  def >>[C](other: JoinRule[B, C]): JoinRule[A, C]

sealed trait ConditionalRule[A] extends Rule[A, Boolean]:
  def and(other: ConditionalRule[A]): ConditionalRule[A]
  def or(other: ConditionalRule[A]): ConditionalRule[A]

final case class Base[A, B](f: A => B) extends JoinRule[A, B]:
  override def >>[C](other: JoinRule[B, C]): JoinRule[A, C] = Base(f andThen other.f)

final case class Policy[A](f: A => Boolean) extends ConditionalRule[A]:
    def and(other: ConditionalRule[A]): ConditionalRule[A] = Policy((a: A) => f(a) && other.f(a))
    def or(other: ConditionalRule[A]): ConditionalRule[A] = Policy((a: A) => f(a) || other.f(a))


object Rule:

  given policyConv[A]: Conversion[A => Boolean, Policy[A]] with
    def apply(f: A => Boolean): Policy[A] = Policy(f)

  given baseConv[A, B]: Conversion[A => B, Base[A, B]] with
    def apply(f: A => B): Base[A, B] = Base(f)

  def conditional[A](f: A => Boolean): ConditionalRule[A] = Policy(f)

  def base[A, B](f: A => B): JoinRule[A, B] = Base(f)

object Test:
  import Rule.given

  ((x: Int) => x + 1) >> ((x: Int) => x * 2) executeOn 1
  ((x: Int) => x > 0) and ((x: Int) => x < 2)
  ((x: Int) => x > 0) or ((x: Int) => x < 0)