package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.ProtocolName
import edu.cornell.cs.apl.viaduct.syntax.SpecializedProtocol
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.IfNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ObjectDeclarationArgumentNode
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
    fun protocols(): List<SpecializedProtocol>
    fun viableProtocols(node: LetNode): Set<Protocol>
    fun viableProtocols(node: DeclarationNode): Set<Protocol>
    fun viableProtocols(node: ParameterNode): Set<Protocol>

    fun availableProtocols(): Set<ProtocolName>

    /** TODO: This interface can likely be simplified by collapsing DeclarationNode and ObjectDeclarationArgumentNode
    together by taking in [ObjectDeclaration] interface
     **/
    fun viableProtocols(node: ObjectDeclarationArgumentNode): Set<Protocol>
    fun constraint(node: LetNode): SelectionConstraint {
        return Literal(true)
    }

    fun constraint(node: DeclarationNode): SelectionConstraint {
        return Literal(true)
    }

    fun constraint(node: ParameterNode): SelectionConstraint {
        return Literal(true)
    }

    fun guardVisibilityConstraint(protocol: Protocol, node: IfNode): SelectionConstraint {
        return Literal(true)
    }
}

/** Union of protocol selectors. [unions] takes a number of selectors and implements their collective union. */

open class UnionProtocolFactory(private val selectors: Set<ProtocolFactory>) : ProtocolFactory {
    constructor(vararg selectors: ProtocolFactory) : this(selectors.toSet())

    private val protocolMap: Map<ProtocolName, ProtocolFactory>

    init {
        val currentProtocols: MutableSet<ProtocolName> = mutableSetOf()
        val initProtocolMap: MutableMap<ProtocolName, ProtocolFactory> = mutableMapOf()
        for (selector in selectors) {
            val selectorProtocols = selector.availableProtocols()
            if (selectorProtocols.all { !currentProtocols.contains(it) }) {
                currentProtocols.addAll(selectorProtocols)
                initProtocolMap.putAll(selectorProtocols.map { it to selector })
            } else {
                // TODO: better exception type
                throw Error("union protocol factory has clashing selectors")
            }
        }

        protocolMap = initProtocolMap
    }

    override fun protocols() = selectors.map { it.protocols() }.flatten()

    override fun availableProtocols(): Set<ProtocolName> = protocolMap.keys

    override fun viableProtocols(node: LetNode): Set<Protocol> =
        selectors.fold(setOf()) { acc, sel -> acc.union(sel.viableProtocols(node)) }

    override fun viableProtocols(node: DeclarationNode): Set<Protocol> =
        selectors.fold(setOf()) { acc, sel -> acc.union(sel.viableProtocols(node)) }

    override fun viableProtocols(node: ParameterNode): Set<Protocol> =
        selectors.fold(setOf()) { acc, sel -> acc.union(sel.viableProtocols(node)) }

    override fun viableProtocols(node: ObjectDeclarationArgumentNode): Set<Protocol> =
        selectors.fold(setOf()) { acc, sel -> acc.union(sel.viableProtocols(node)) }

    override fun constraint(node: LetNode): SelectionConstraint =
        selectors.map { sel -> sel.constraint(node) }.ands()

    override fun constraint(node: DeclarationNode): SelectionConstraint =
        selectors.map { sel -> sel.constraint(node) }.ands()

    override fun guardVisibilityConstraint(protocol: Protocol, node: IfNode): SelectionConstraint =
        protocolMap[protocol.protocolName]?.guardVisibilityConstraint(protocol, node)
            ?: throw Error("ProtocolFactory: unknown protocol ${protocol.protocolName}")
}
