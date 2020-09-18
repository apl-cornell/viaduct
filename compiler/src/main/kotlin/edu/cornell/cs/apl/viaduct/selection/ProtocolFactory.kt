package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.IfNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ParameterNode

/**
 *
 * This interface specifies factories for protocol selection.
 * The role of a factory is twofold:
 * First, it outputs the set of viable protocols which may be selected at that node.
 *** A protocol is viable for a node if it satisfies the selector's custom syntactic restrictions.
 * Second, it outputs a constraint for that node (by default, the constraint is trivial).
 *** The constraints encode all of the other interdependencies present in protocol selection.

 */

interface ProtocolFactory {
    fun viableProtocols(node: LetNode): Set<Protocol>
    fun viableProtocols(node: DeclarationNode): Set<Protocol>
    fun viableProtocols(node: ParameterNode): Set<Protocol>
    fun constraint(node: LetNode): SelectionConstraint {
        return Literal(true)
    }

    fun constraint(node: DeclarationNode): SelectionConstraint {
        return Literal(true)
    }

    fun constraint(node: ParameterNode): SelectionConstraint {
        return Literal(true)
    }

    fun constraint(node: IfNode): SelectionConstraint {
        return Literal(true)
    }
}

/** Union of protocol selectors. [unions] takes a number of selectors and implements their collective union. */

fun unions(vararg selectors: ProtocolFactory): ProtocolFactory = object : ProtocolFactory {
    override fun viableProtocols(node: LetNode): Set<Protocol> =
        selectors.fold(setOf()) { acc, sel -> acc.union(sel.viableProtocols(node)) }

    override fun viableProtocols(node: DeclarationNode): Set<Protocol> =
        selectors.fold(setOf()) { acc, sel -> acc.union(sel.viableProtocols(node)) }

    override fun viableProtocols(node: ParameterNode): Set<Protocol> =
        selectors.fold(setOf()) { acc, sel -> acc.union(sel.viableProtocols(node)) }

    override fun constraint(node: LetNode): SelectionConstraint =
        selectors.fold<ProtocolFactory, SelectionConstraint>(Literal(true)) { acc, sel ->
            And(
                acc,
                sel.constraint(node)
            )
        }

    override fun constraint(node: DeclarationNode): SelectionConstraint =
        selectors.fold<ProtocolFactory, SelectionConstraint>(Literal(true)) { acc, sel ->
            And(
                acc,
                sel.constraint(node)
            )
        }

    override fun constraint(node: IfNode): SelectionConstraint =
        selectors.fold<ProtocolFactory, SelectionConstraint>(Literal(true)) { acc, sel ->
            And(
                acc,
                sel.constraint(node)
            )
        }
}
