package edu.cornell.cs.apl.viaduct.algebra.solver

import edu.cornell.cs.apl.viaduct.algebra.HeytingAlgebra
import edu.cornell.cs.apl.viaduct.util.dataflow.DataFlowEdge
import org.jgrapht.graph.DefaultEdge

/** Maps incoming values `input` to `c.imply(input)`. */
internal class ImplyEdge<A : HeytingAlgebra<A>>(private val antecedent: A) : DefaultEdge(), DataFlowEdge<A> {
    override fun propagate(input: A): A {
        return antecedent.imply(input)
    }

    override fun toString(): String {
        return "$antecedent → _"
    }
}
