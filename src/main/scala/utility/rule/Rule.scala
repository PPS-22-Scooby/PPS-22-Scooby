package org.unibo.scooby
package utility.rule

/**
 * The `Rule` enum represents a rule that transforms a value of type `A` to a value of type `B`.
 *
 * @tparam A the input type of the rule
 * @tparam B the output type of the rule
 */
enum Rule[A, B]:

  /**
   * Base case of the `Rule` enum which wraps a function `f` of type `A => B`.
   *
   * @param f the function that transforms a value of type `A` to a value of type `B`
   */
  case Base(f: A => B)

  /**
   * Composes this rule with another rule to create a new rule that transforms a value of type `A` to a value of type `C`.
   * This method is used as proxy of `union`:
   *
   *{{{
   *   val composedRule = rule1 >> rule2
   *}}}
   *
   * an be written as:
   * {{{
   *   val composedRule = rule1 union rule2
   * }}}
   *
   * @param other the rule to compose with this rule
   * @tparam C the output type of the composed rule
   * @return a new rule that transforms a value of type `A` to a value of type `C`
   * */
  def >>[C](other: Rule[B, C]): Rule[A, C] = union(other)

  /**
   * Composes this rule with another rule to create a new rule that transforms a value of type `A` to a value of type `C`.
   *
   * @param other the rule to union with this rule
   * @tparam C the output type of the unioned rule
   * @return a new rule that transforms a value of type `A` to a value of type `C`
   */
  def union[C](other: Rule[B, C]): Rule[A, C] = (this, other) match
    case (Base(f), Base(g)) => Base(f andThen g)

  /**
   * Executes this rule on the given value.
   *
   * @param value the value to transform
   * @return the result of applying the rule to the value
   */
  def executeOn(value: A): B = this match
    case Base(f) => f(value)

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

  /**
   * Given instance of `Conversion` that allows implicit conversion from a function `A => B` to a `Rule[A, B]`.
   */
  given converter[A,B]: Conversion[A => B, Rule[A,B]] with
    def apply(f: A => B): Rule[A, B] = Rule(f)

  extension [A, B] (f: A => B)
    /**
     * Converts the function `f` to a `Rule` using the given conversion.
     *
     * @param conv the implicit conversion from `A => B` to `Rule[A, B]`
     * @return a new `Rule` that wraps the function `f`
     */
    def rule(implicit conv: Conversion[A => B, Rule[A,B]]): Rule[A, B] = conv(f)
