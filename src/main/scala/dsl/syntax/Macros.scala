package org.unibo.scooby
package dsl.syntax

import scala.compiletime.{summonFrom, error}

/**
 * Private macro to check if a given value of type [[T]] can be summoned from here.
 * @tparam T type of the value to be summoned
 * @return [[true]] if the [[given]] value exists, [[false]] otherwise
 */
private inline def isInContext[T]: Boolean =
	summonFrom:
		case given T => true
		case _ => false

/**
 * Utility macro to catch if a given value of type [[T]] exists in this scope. This can prevent DSL usages
 * like `scrape: ... scrape: ...` at compile time
 * @param contextName name of the context, used to generate the error message
 * @tparam T type of the context to check
 */
inline def catchRecursiveCtx[T](inline contextName: String): Unit =
	if isInContext[T] then error("\"" + contextName +"\" keyword cannot be placed inside another \"" + contextName +"\"")
