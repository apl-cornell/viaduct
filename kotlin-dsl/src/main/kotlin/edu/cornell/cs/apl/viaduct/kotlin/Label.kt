package edu.cornell.cs.apl.viaduct.kotlin

import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty

sealed class Label {
    infix fun or(other: Label): Label = Or(this, other)

    infix fun and(other: Label): Label = And(this, other)

    val c: Label
        get() = ConfidentialityProjection(this)

    val i: Label
        get() = IntegrityProjection(this)

    companion object {
        val weakest: Label = Weakest

        val strongest: Label = Strongest
    }
}

/** An actor with an associated security label. */
class Principal internal constructor(internal val displayName: String) : Label()

internal object Weakest : Label()

internal class Or(val argument1: Label, val argument2: Label) : Label()

internal class And(val argument1: Label, val argument2: Label) : Label()

internal object Strongest : Label()

internal class ConfidentialityProjection(val label: Label) : Label()

internal class IntegrityProjection(val label: Label) : Label()

val principal = PropertyDelegateProvider { _: Any?, property ->
    val result = Principal(property.name)
    ReadOnlyProperty<Any?, Principal> { _, _ -> result }
}
