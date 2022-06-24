package io.github.apl_cornell.viaduct.algebra.solver

import io.github.apl_cornell.viaduct.algebra.Lattice
import io.github.apl_cornell.viaduct.util.dataflow.DataFlowEdge
import org.jgrapht.graph.DefaultEdge

/** Joins the incoming value with a constant before passing it on. */
internal class JoinEdge<A : Lattice<A>>(private val constant: A) : DefaultEdge(), DataFlowEdge<A> {
    override fun propagate(input: A): A {
        return constant.join(input)
    }

    override fun toString(): String {
        return "$constant ∨ _"
    }
}
