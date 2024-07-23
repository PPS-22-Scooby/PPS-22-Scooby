package dsl.syntax

import scala.compiletime.{summonFrom, error}

private inline def isInContext[T]: Boolean =
	summonFrom:
		case given T => true
		case _ => false


inline def catchRecursiveCtx[T](inline contextName: String): Unit =
	if isInContext[T] then error("\"" + contextName +"\" keyword cannot be placed inside another \"" + contextName +"\"")

inline def catchInvalidCtx[T](inline message: String): Unit =
	if !isInContext[T] then error(message)
