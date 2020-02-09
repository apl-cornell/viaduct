package edu.cornell.cs.apl.viaduct.syntax.types

import edu.cornell.cs.apl.viaduct.syntax.Operator
import kotlinx.collections.immutable.ImmutableList

/** The type of an [Operator]. */
data class OperatorType(
    val arguments: ImmutableList<ValueType>,
    val result: ValueType
) : Type
