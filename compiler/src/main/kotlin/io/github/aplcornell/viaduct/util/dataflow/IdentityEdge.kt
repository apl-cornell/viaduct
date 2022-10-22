package io.github.aplcornell.viaduct.util.dataflow

import org.jgrapht.graph.DefaultEdge

/** An edge that passes values through unmodified. */
class IdentityEdge<A> : DefaultEdge(), DataFlowEdge<A> {
    override fun propagate(input: A): A {
        return input
    }

    override fun toString(): String {
        return ""
    }
}
