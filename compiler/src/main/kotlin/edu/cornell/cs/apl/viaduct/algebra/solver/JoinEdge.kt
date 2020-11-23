package edu.cornell.cs.apl.viaduct.algebra.solver

import edu.cornell.cs.apl.viaduct.algebra.Lattice
import edu.cornell.cs.apl.viaduct.util.dataflow.DataFlowEdge
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
