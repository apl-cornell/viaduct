package edu.cornell.cs.apl.viaduct.algebra.solver2

/** Thrown if a [Term] is not allowed to appear in [Constraint]s. */
class IllegalTermException(term: Term<*, *>) : IllegalArgumentException("Term $term cannot appear in constraints.")
