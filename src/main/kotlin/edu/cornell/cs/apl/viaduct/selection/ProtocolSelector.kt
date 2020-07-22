package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.Variable
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode

/**
This interface specifies selectors for protocol selection. Selectors are given a partial assignment
of protocols selected so far, and either a LetNode or DeclarationNode,
and outputs a set of protocols which can implement that node.

Protocol selectors do NOT have to enforce that the selected protocols have enough authority to implement the node.
This invariant is enforced during protocol selection.

 **/

interface ProtocolSelector {
    fun select(node: LetNode, currentAssignment: Map<Variable, Protocol>): Set<Protocol>
    fun select(node: DeclarationNode, currentAssignment: Map<Variable, Protocol>): Set<Protocol>
}

/* Union of protocol selectors. [unions] takes a number of selectors and implements their collective union. */

fun unions(vararg selectors: ProtocolSelector): ProtocolSelector = object : ProtocolSelector {
    override fun select(node: LetNode, currentAssignment: Map<Variable, Protocol>): Set<Protocol> =
        selectors.fold(setOf()) { acc, sel -> acc.union(sel.select(node, currentAssignment)) }

    override fun select(node: DeclarationNode, currentAssignment: Map<Variable, Protocol>): Set<Protocol> =
        selectors.fold(setOf()) { acc, sel -> acc.union(sel.select(node, currentAssignment)) }
}
