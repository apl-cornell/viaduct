package edu.cornell.cs.apl.viaduct.imp.types

import edu.cornell.cs.apl.viaduct.imp.ast2.Operator

/** The type of an [Operator]. */
data class OperatorType(val arguments: List<ValueType>, val result: ValueType) : Type
