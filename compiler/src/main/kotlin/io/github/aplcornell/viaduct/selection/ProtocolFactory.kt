package io.github.aplcornell.viaduct.selection

import io.github.aplcornell.viaduct.syntax.Protocol
import io.github.aplcornell.viaduct.syntax.intermediate.IfNode
import io.github.aplcornell.viaduct.syntax.intermediate.Node
import io.github.aplcornell.viaduct.syntax.intermediate.UpdateNode
import io.github.aplcornell.viaduct.syntax.intermediate.VariableDeclarationNode
import io.github.aplcornell.viaduct.util.unions

/**
 * Generates a list of [Protocol]s that can implement a given [Node],
 * along with constraints that specify when using these protocols is valid.
 *
 * The factory needs to ensure protocols returned for a node can implement
 * the operations in that node. However, it does not need to ensure the
 * protocols have enough authority to implement the node securely.
 */
interface ProtocolFactory {
    /** Returns the set of protocols that can implement [node]. */
    fun viableProtocols(node: VariableDeclarationNode): Set<Protocol>

    fun constraint(node: VariableDeclarationNode): SelectionConstraint {
        return Literal(true)
    }

    fun constraint(node: UpdateNode): SelectionConstraint {
        return Literal(true)
    }

    // TODO: this method becomes unnecessary when Viaduct handles control flow generically.
    fun guardVisibilityConstraint(
        protocol: Protocol,
        node: IfNode,
    ): SelectionConstraint {
        return Literal(true)
    }
}

/** Combines given factories into a single factory that returns protocols from all of them. */
fun Iterable<ProtocolFactory>.unions(): ProtocolFactory =
    object : ProtocolFactory {
        private val factories = this@unions.toList()

        override fun viableProtocols(node: VariableDeclarationNode): Set<Protocol> = factories.map { it.viableProtocols(node) }.unions()

        override fun constraint(node: VariableDeclarationNode): SelectionConstraint = factories.map { it.constraint(node) }.ands()

        override fun constraint(node: UpdateNode): SelectionConstraint = factories.map { it.constraint(node) }.ands()

        override fun guardVisibilityConstraint(
            protocol: Protocol,
            node: IfNode,
        ): SelectionConstraint = factories.map { it.guardVisibilityConstraint(protocol, node) }.ands()
    }

/** Restricts the given factory to protocols that satisfy [predicate]. */
fun ProtocolFactory.filter(predicate: (Protocol) -> Boolean): ProtocolFactory =
    object : ProtocolFactory by this {
        override fun viableProtocols(node: VariableDeclarationNode): Set<Protocol> =
            this@filter.viableProtocols(node).filter(predicate).toSet()
    }
