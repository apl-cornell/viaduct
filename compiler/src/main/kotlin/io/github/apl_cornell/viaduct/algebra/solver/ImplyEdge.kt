package io.github.apl_cornell.viaduct.algebra.solver

import io.github.apl_cornell.viaduct.algebra.HeytingAlgebra
import io.github.apl_cornell.viaduct.util.dataflow.DataFlowEdge
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
