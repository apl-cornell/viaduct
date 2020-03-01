package edu.cornell.cs.apl.viaduct.syntax.intermediate.attributes

import java.util.IdentityHashMap
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

// TODO: move this (and everything else) to its own module since none of this is Viaduct specific.
// TODO: make these thread safe.
// TODO: garbage collection?

/**
 * An attribute of type [T] for nodes of type [Node].
 *
 * This class is meant to be used as a property delegate:
 * ```
 * val height: Int by attribute { ... }
 * ```
 */
abstract class Attribute<in Node, out T> : (Node) -> T, ReadOnlyProperty<Node, T> {
    final override fun getValue(thisRef: Node, property: KProperty<*>): T =
        this(thisRef)
}

/**
 * Defines an [Attribute] of type [T] for nodes of type [Node].
 * The value of the attribute for a given [Node] is determined by the function [f].
 *
 * Calls to [f] are cached, so there is at most one call to [f] for any given [Node].
 * If the value of the attribute for a [Node] is never requested, then there are no calls to [f]
 * for that [Node]. Note that [Node] equality is determined by object identity, not [Any.equals].
 *
 * @throws CycleInAttributeDefinitionException if [f]`(node)` depends on [f]`(node)` for some `node`.
 *
 * @see Attribute
 */
fun <Node, T> attribute(f: Node.() -> T): Attribute<Node, T> =
    CachedAttribute(f)

private class CachedAttribute<in Node, out T>(private val f: (Node) -> T) : Attribute<Node, T>() {
    private val cache: MutableMap<Node, Option<T>> = IdentityHashMap()

    override operator fun invoke(node: Node): T {
        val previousValue = cache.putIfAbsent(node, None)
        return if (previousValue != null) {
            when (previousValue) {
                is Some<T> ->
                    previousValue.value
                is None ->
                    throw CycleInAttributeDefinitionException()
            }
        } else {
            val value = f(node)
            cache[node] = Some(value)
            value
        }
    }
}

/** Thrown when a cycle is (dynamically) detected in an [Attribute] definition. */
class CycleInAttributeDefinitionException :
    RuntimeException("Cycle detected in attribute definition.")
