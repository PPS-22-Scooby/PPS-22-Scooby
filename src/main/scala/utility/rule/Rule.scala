package org.unibo.scooby
package utility.rule

import scala.language.implicitConversions


/**
 * A trait representing a rule that can be applied to a value of type A to produce a value of type B.
 * @tparam A the type of the input to the rule
 * @tparam B the type of the output of the rule
 */
trait Rule[A, B]:
  /**
   * The function that defines the rule.
   * @return a function from A to B
   */
  def f : A => B

  /**
   * Executes the rule on a given value.
   * @param value the value to apply the rule to
   * @return the result of applying the rule
   */
  def executeOn(value: A): B = f(value)

/**
 * A trait representing a rule that can be joined with another rule.
 * @tparam A the type of the input to the rule
 * @tparam B the type of the output of the rule
 */
sealed trait JoinRule[A, B] extends Rule[A, B]:
  /**
   * Joins this rule with another rule.
   * @param other the other rule to join with
   * @tparam C the type of the output of the other rule
   * @return a new rule that represents the composition of this rule and the other rule
   */
  def >>[C](other: JoinRule[B, C]): JoinRule[A, C]

/**
 * A trait representing a conditional rule that can be applied to a value of type A to produce a Boolean value.
 * @tparam A the type of the input to the rule
 */
sealed trait ConditionalRule[A] extends Rule[A, Boolean]:
  /**
   * Combines this rule with another rule using logical AND.
   * @param other the other rule to combine with
   * @return a new rule that represents the logical AND of this rule and the other rule
   */
  def and(other: ConditionalRule[A]): ConditionalRule[A]

  /**
   * Combines this rule with another rule using logical OR.
   * @param other the other rule to combine with
   * @return a new rule that represents the logical OR of this rule and the other rule
   */
  def or(other: ConditionalRule[A]): ConditionalRule[A]

/**
 * A case class representing a base rule that can be joined with another rule.
 * @tparam A the type of the input to the rule
 * @tparam B the type of the output of the rule
 */
final case class Base[A, B](f: A => B) extends JoinRule[A, B]:
  /**
   * Joins this rule with another rule.
   * @param other the other rule to join with
   * @tparam C the type of the output of the other rule
   * @return a new rule that represents the composition of this rule and the other rule
   */
  override def >>[C](other: JoinRule[B, C]): JoinRule[A, C] = Base(f andThen other.f)

/**
 * A case class representing a policy rule that can be combined with another rule using logical AND or OR.
 * @tparam A the type of the input to the rule
 */
final case class Policy[A](f: A => Boolean) extends ConditionalRule[A]:
  /**
   * Combines this rule with another rule using logical AND.
   * @param other the other rule to combine with
   * @return a new rule that represents the logical AND of this rule and the other rule
   */
  def and(other: ConditionalRule[A]): ConditionalRule[A] = Policy((a: A) => f(a) && other.f(a))

  /**
   * Combines this rule with another rule using logical OR.
   * @param other the other rule to combine with
   * @return a new rule that represents the logical OR of this rule and the other rule
   */
  def or(other: ConditionalRule[A]): ConditionalRule[A] = Policy((a: A) => f(a) || other.f(a))

/**
 * An object representing a set of utility methods for creating and manipulating rules.
 */
object Rule:

  /**
   * A given conversion from a function to a policy rule.
   * @tparam A the type of the input to the function
   */
  given policyConv[A]: Conversion[A => Boolean, Policy[A]] with
    /**
     * Converts a function to a policy rule.
     * @param f the function to convert
     * @return a policy rule that represents the function
     */
    def apply(f: A => Boolean): Policy[A] = Policy(f)

  /**
   * A given conversion from a function to a base rule.
   * @tparam A the type of the input to the function
   * @tparam B the type of the output of the function
   */
  given baseConv[A, B]: Conversion[A => B, Base[A, B]] with
    /**
     * Converts a function to a base rule.
     * @param f the function to convert
     * @return a base rule that represents the function
     */
    def apply(f: A => B): Base[A, B] = Base(f)

  /**
   * Creates a conditional rule from a function.
   * @param f the function to convert to a rule
   * @tparam A the type of the input to the function
   * @return a conditional rule that represents the function
   */
  def conditional[A](f: A => Boolean): ConditionalRule[A] = Policy(f)

  /**
   * Creates a base rule from a function.
   * @param f the function to convert to a rule
   * @tparam A the type of the input to the function
   * @tparam B the type of the output of the function
   * @return a base rule that represents the function
   */
  def base[A, B](f: A => B): JoinRule[A, B] = Base(f)
