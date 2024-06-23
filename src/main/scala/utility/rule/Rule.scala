package org.unibo.scooby
package utility.rule

/**
 * The `Rule` enum represents a rule that transforms a value of type `A` to a value of type `B`.
 *
 * @tparam A the input type of the rule
 * @tparam B the output type of the rule
 */
enum Rule[A, B]:

  case Base(f: A => B)
  case Policy(f: A => Boolean)
  case Invalid()
  
  def and(other: Rule[A, Boolean]): Rule[A, Boolean] = (this, other) match
    case (Policy(p1), Policy(p2)) => Policy((a: A) => p1(a) && p2(a))
    case _ => Invalid()

  def or(other: Rule[A, Boolean]): Rule[A, Boolean] = (this, other) match
    case (Policy(p1), Policy(p2)) => Policy((a: A) => p1(a) || p2(a))
    case _ => Invalid()

  def >>[C](other: Rule[B, C]): Rule[A, C] = (this, other) match
    case (Base(f), Base(g)) => Base(f andThen g)
    case _ => Invalid()

  def executeOn(value: A): Option[?] = this match
    case Base(f) => Some(f(value))
    case Policy(p) => Some(p(value))
    case Invalid() => None

object Rule:
  /**
   * Creates a new `Rule` from a function.
   *
   * @param f the function to wrap in a `Rule`
   * @tparam A the input type of the rule
   * @tparam B the output type of the rule
   * @return a new `Rule` that wraps the function `f`
   */
  def apply[A, B](f: A => B): Rule[A, B] = Base(f)

  def policy[A](f: A => Boolean): Rule[A, Boolean] = Policy(f)

  /**
   * Given instance of `Conversion` that allows implicit conversion from a function `A => B` to a `Rule[A, B]`.
   */
  given baseConverter[A,B]: Conversion[A => B, Rule[A,B]] with
    def apply(f: A => B): Rule[A, B] = f match
      case g: Function[A, Boolean] => Policy(g)
      case _ => Base(f)


  extension [A, B] (f: A => B)
    /**
     * Converts the function `f` to a `Rule` using the given conversion.
     *
     * @param conv the implicit conversion from `A => B` to `Rule[A, B]`
     * @return a new `Rule` that wraps the function `f`
     */
    def rule(using conv: Conversion[A => B, Rule[A,B]]): Rule[A, B] = conv(f)

