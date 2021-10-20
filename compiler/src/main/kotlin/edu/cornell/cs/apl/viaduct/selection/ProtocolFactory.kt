package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.IfNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.Node
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ParameterNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode
import edu.cornell.cs.apl.viaduct.util.unions

/**
 * Generates a list of [Protocol]s that can implement a given [Node],
 * along with constraints that specify when using these protocols is valid.
 *
 * The factory needs to ensure protocols returned for a node can implement
 * the operations in that node. However, it does not need to ensure the
 * protocols have enough authority to implement the node securely.
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

    fun constraint(node: UpdateNode): SelectionConstraint {
        return Literal(true)
    }

    fun constraint(node: ParameterNode): SelectionConstraint {
        return Literal(true)
    }

    // TODO: this method becomes unnecessary when Viaduct handles control flow generically.
    fun guardVisibilityConstraint(protocol: Protocol, node: IfNode): SelectionConstraint {
        return Literal(true)
    }
}

/** Combines given factories into a single factory that returns protocols from all of them. */
fun Iterable<ProtocolFactory>.unions(): ProtocolFactory =
    object : ProtocolFactory {
        private val factories = this@unions.toList()

        override fun viableProtocols(node: LetNode): Set<Protocol> =
            factories.map { it.viableProtocols(node) }.unions()

        override fun viableProtocols(node: DeclarationNode): Set<Protocol> =
            factories.map { it.viableProtocols(node) }.unions()

        override fun viableProtocols(node: ParameterNode): Set<Protocol> =
            factories.map { it.viableProtocols(node) }.unions()

        override fun constraint(node: LetNode): SelectionConstraint =
            factories.map { it.constraint(node) }.ands()

        override fun constraint(node: DeclarationNode): SelectionConstraint =
            factories.map { it.constraint(node) }.ands()

        override fun constraint(node: UpdateNode): SelectionConstraint =
            factories.map { it.constraint(node) }.ands()

        override fun guardVisibilityConstraint(protocol: Protocol, node: IfNode): SelectionConstraint =
            factories.map { it.guardVisibilityConstraint(protocol, node) }.ands()
    }

/** Restricts the given factory to protocols that satisfy [predicate]. */
fun ProtocolFactory.filter(predicate: (Protocol) -> Boolean): ProtocolFactory =
    object : ProtocolFactory by this {
        override fun viableProtocols(node: LetNode): Set<Protocol> =
            this@filter.viableProtocols(node).filter(predicate).toSet()

        override fun viableProtocols(node: DeclarationNode): Set<Protocol> =
            this@filter.viableProtocols(node).filter(predicate).toSet()

        override fun viableProtocols(node: ParameterNode): Set<Protocol> =
            this@filter.viableProtocols(node).filter(predicate).toSet()
    }
