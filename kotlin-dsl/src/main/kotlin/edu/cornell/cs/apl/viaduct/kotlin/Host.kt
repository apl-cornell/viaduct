package edu.cornell.cs.apl.viaduct.kotlin

import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty

class Host internal constructor(internal val displayName: String, val label: Label)

fun host(label: Label?) = PropertyDelegateProvider { _: Any?, property ->
    val displayName = property.name
    val result = Host(displayName, label ?: Principal(displayName))
    ReadOnlyProperty<Any?, Host> { _, _ -> result }
}
