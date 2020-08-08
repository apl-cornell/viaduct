package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode

/**
This interface specifies selectors for protocol selection.
The role of a selector is twofold:
    - first, it outputs the set of viable protocols which may be selected at that node.
          A protocol is viable for a node if it satisfies both the information flow constraints
          and syntactic restrictions for that node.
    - Second, it outputs a constraints for that node. The constraints encode all of the other interdependencies present
      in protocol selection.

Protocol selectors do NOT have to enforce that the selected protocols have enough authority to implement the node.
This invariant is enforced during protocol selection. However for efficiency it is better for them so.

 **/

interface ProtocolSelector {
    fun viableProtocols(node: LetNode): Set<Protocol>
    fun viableProtocols(node: DeclarationNode): Set<Protocol>
    fun constraint(node: LetNode): SelectionConstraint {
        return Literal(true)
    }
    fun constraint(node: DeclarationNode): SelectionConstraint {
        return Literal(true)
    }
}

/* Union of protocol selectors. [unions] takes a number of selectors and implements their collective union. */

fun unions(vararg selectors: ProtocolSelector): ProtocolSelector = object : ProtocolSelector {
    override fun viableProtocols(node: LetNode): Set<Protocol> =
        selectors.fold(setOf()) { acc, sel -> acc.union(sel.viableProtocols(node)) }

    override fun viableProtocols(node: DeclarationNode): Set<Protocol> =
        selectors.fold(setOf()) { acc, sel -> acc.union(sel.viableProtocols(node)) }

    override fun constraint(node: LetNode): SelectionConstraint =
        selectors.fold<ProtocolSelector, SelectionConstraint>(Literal(true)) { acc, sel -> And(acc, sel.constraint(node)) }

    override fun constraint(node: DeclarationNode): SelectionConstraint =
        selectors.fold<ProtocolSelector, SelectionConstraint>(Literal(true)) { acc, sel -> And(acc, sel.constraint(node)) }
}
