package io.github.aplcornell.viaduct.attributes

import java.util.concurrent.ConcurrentHashMap
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

// TODO: make these thread safe.
// TODO: garbage collection?
// TODO: add more information to errors (e.g. property name which we know).

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

/**
 * Defines an [Attribute] where the value of each [Node] is determined by the contributions from
 * the other nodes. For every node, [f] specifies the contribution of that node to the other nodes.
 * All nodes in [tree] are traversed to collect the attributes for each node.
 *
 * The tree is traversed lazily—only when the value of _any_ node is demanded.
 * Note that it is safe for [f] to use other attributes, but [f] should not depend on the attribute
 * being defined.
 */
// TODO: improve implementation using ideas from
//  [Extending Attribute Grammars with Collection Attributes – Evaluation and Applications](https://www.ieee-scam.org/2007/papers/40.pdf)
fun <Node : TreeNode<Node>, T> collectedAttribute(
    tree: Tree<Node, Node>,
    f: (Node) -> Iterable<Pair<Node, T>>,
): Attribute<Node, Set<T>> {
    val attributes: MutableMap<Node, MutableSet<T>> = mutableMapOf()
    fun visit(node: Node) {
        f(node).forEach { attributes.getOrPut(it.first) { mutableSetOf() }.add(it.second) }
        node.children.forEach(::visit)
    }

    // Traverse the tree lazily (only when any value is demanded)
    val result: Map<Node, Set<T>> by lazy { visit(tree.root); attributes }
    return UncachedAttribute { result.getOrDefault(it, setOf()) }
}

private class CachedAttribute<in Node, out T>(private val f: (Node) -> T) : Attribute<Node, T>() {
    private val cache: MutableMap<Node, AttributeValue<Option<T>>> = ConcurrentHashMap()

    override operator fun invoke(node: Node): T {
        val attributeValue = cache.getOrPut(node) { AttributeValue(None) }
        return synchronized(attributeValue) {
            when (val valueOption = attributeValue.currentValue) {
                is Some<T> ->
                    valueOption.value
                is None ->
                    if (attributeValue.isVisited) {
                        throw CycleInAttributeDefinitionException()
                    } else {
                        attributeValue.isVisited = true
                        val newValue = f(node)
                        attributeValue.currentValue = Some(newValue)
                        attributeValue.finalize()
                        newValue
                    }
            }
        }
    }
}

private class UncachedAttribute<in Node, out T>(private val f: (Node) -> T) : Attribute<Node, T>() {
    override fun invoke(node: Node): T = f(node)
}

/** Thrown when a cycle is (dynamically) detected in an [Attribute] definition. */
class CycleInAttributeDefinitionException :
    RuntimeException("Cycle detected in attribute definition.")
