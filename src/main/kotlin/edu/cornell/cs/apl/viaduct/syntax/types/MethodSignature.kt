package edu.cornell.cs.apl.viaduct.syntax.types

import kotlinx.collections.immutable.ImmutableList

data class MethodSignature(
    val arguments: ImmutableList<Type>,
    val result: Type
) : Type
