package edu.cornell.cs.apl.attributes

import java.util.IdentityHashMap

/**
 * Defines an [Attribute] of type [T] for nodes of type [Node].
 * Unlike with [attribute], attribute definitions are allowed to be circular.
 *
 * The value of the attribute is computed by the function [f], which may itself use the value of
 * the attribute. Attribute values are initialized to [initial]. The attribute (and any circular
 * attributes on which it depends) are evaluated until no value changes (i.e., a fixed point is
 * reached). The final result is cached so that subsequent evaluations are fast and return the same
 * value.
 *
 * @see attribute
 * @see Attribute
 */
fun <Node, T> circularAttribute(initial: T, f: Node.() -> T): Attribute<Node, T> =
    CircularAttribute(initial, f)

/** Global state for the circular attribute evaluation algorithm. */
// TODO: global state is horrible, but there are no other algorithms...
private object EvaluationState {
    /** Are we currently evaluating a circle of attributes? */
    var IN_CIRCLE = false

    /** Has an attribute on the current circle changed value since the last time it was computed? */
    var CHANGE = false

    /** Are we in the final clean-up pass around the circle? */
    var READY = false

    /** Sets the state to the initial default. */
    fun reset() {
        IN_CIRCLE = false
        CHANGE = false
        READY = false
    }
}

/**
 * Implements the algorithm from
 * [Circular reference attributed grammars—their evaluation and applications](sciencedirect.com/science/article/pii/S0167642307000767).
 *
 * This code is shamelessly copied from the Kiama library.
 * See [their implementation](http://bit.ly/kiama-circular-attributes).
 */
private class CircularAttribute<in Node, out T>(
    private val initial: T,
    private val f: (Node) -> T
) : Attribute<Node, T>() {
    private val cache: MutableMap<Node, AttributeValue<T>> = IdentityHashMap()

    /**
     * Run [f] in a safe manner, resetting the global [EvaluationState] if it throws an exception.
     */
    fun safeF(node: Node): T =
        try {
            f(node)
        } catch (e: Throwable) {
            EvaluationState.reset()
            throw e
        }

    override fun invoke(node: Node): T {
        val attributeValue = cache.getOrPut(node) { AttributeValue(initial) }

        if (attributeValue.isFinalized) {
            return attributeValue.value
        }

        when {
            !EvaluationState.IN_CIRCLE -> {
                // This is the first evaluation of a circular attribute occurrence, so enter
                // a fixed-point computation that computes it and all dependent attribute
                // occurrences until they stabilise.

                EvaluationState.IN_CIRCLE = true
                attributeValue.isVisited = true

                do {
                    // Evaluate the attribute occurrence once. Compare the value that is
                    // computed with the previous value. If they are the same,
                    // we are done, since it and all dependent occurrences have stabilised.
                    // If the values are different, cache the new one and repeat.

                    EvaluationState.CHANGE = false
                    val newValue = safeF(node)
                    if (attributeValue.currentValue != newValue) {
                        EvaluationState.CHANGE = true
                        attributeValue.currentValue = newValue
                    }
                } while (EvaluationState.CHANGE)

                // The value of this attribute at this node has been fully computed.
                attributeValue.finalize()

                // All of the values of dependent attribute occurrences are also final, but have
                // not yet been marked as such. Enter READY mode and go around the circle one more
                // time to finalize them.

                EvaluationState.READY = true
                val newValue = safeF(node)
                assert(attributeValue.value == newValue)
                EvaluationState.READY = false

                // Now we have computed and cached all of the attribute occurrences on the circle
                // so we are done with this one.

                attributeValue.isVisited = false
                EvaluationState.IN_CIRCLE = false
            }

            !attributeValue.isVisited && !EvaluationState.READY -> {
                // We are in a circle but not at the beginning of it. In other words, we are
                // evaluating a circular attribute occurrence on which the initial circular
                // attribute occurrence depends. We reach here if we have not previously
                // visited this occurrence on this iteration of the fixed-point computation.
                // Evaluate this attribute occurrence. As for the initial attribute occurrence
                // above, if the value changes, note that something has changed on the cycle,
                // and cache the new value.

                attributeValue.isVisited = true
                val newValue = safeF(node)
                attributeValue.isVisited = false
                if (attributeValue.currentValue != newValue) {
                    EvaluationState.CHANGE = true
                    attributeValue.currentValue = newValue
                }
            }

            !attributeValue.isVisited && EvaluationState.READY -> {
                // We get to this point if a fixed-point iteration has ended with no changes.
                // The value of the initial attribute occurrence of the circle has stabilised,
                // been cached and marked as computed. Since a fixed-point has been reached,
                // it must be that all dependent attribute occurrences have also stabilised
                // and been cached, so in the READY phase we do one more iteration to mark
                // them as computed as well. This code handles an occurrence that hasn't yet
                // been visited on this last iteration.

                attributeValue.finalize()
                attributeValue.isVisited = true
                val newValue = safeF(node)
                assert(attributeValue.value == newValue)
                attributeValue.isVisited = false
            }

            else -> {
                // We reach this point if we ask for the value of a circular attribute occurrence
                // and we have already visited it in the current fixed-point iteration. We just
                // return the cached value (which happens after the when block) since that is our
                // view of the value of this attribute so far.
            }
        }
        return attributeValue.currentValue
    }
}
