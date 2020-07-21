package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.attributes.attribute
import edu.cornell.cs.apl.viaduct.analysis.InformationFlowAnalysis
import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.protocols.Local
import edu.cornell.cs.apl.viaduct.syntax.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.Variable
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.Node
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProcessDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.QueryNode
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf

/** This class implements a particularly simple but ineffective protocol selection.
    Along with a protocol selector, it takes as input a function [protocolCost] which
    gives a total linear order on protocol cost.

 **/
class SimpleSelection(val selector: ProtocolSelector, val protocolCost: (Protocol) -> Int) {
    fun select(
        processDeclaration: ProcessDeclarationNode,
        nameAnalysis: NameAnalysis,
        informationFlowAnalysis: InformationFlowAnalysis
    ): (Variable) -> Protocol {
        val hostTrustConfiguration = HostTrustConfiguration(nameAnalysis.tree.root)
        var assignment: PersistentMap<Variable, Protocol> = persistentMapOf()
        val protocolSelection = object {
            private val LetNode.possibleProtocols: Set<Protocol> by attribute {
                when (value) {
                    is InputNode ->
                        setOf(Local(value.host.value))
                    is QueryNode -> nameAnalysis.declaration(value).possibleProtocols
                    else ->
                        selector.select(this, assignment)
                }
            }

            private val DeclarationNode.possibleProtocols: Set<Protocol> by attribute {
                selector.select(this, assignment)
            }

            fun possibleProtocols(node: LetNode) = node.possibleProtocols

            fun possibleProtocols(node: DeclarationNode) = node.possibleProtocols
        }

        fun traverse(node: Node) {
            when (node) {
                is LetNode -> {
                    // TODO: proper error class
                    val p = protocolSelection.possibleProtocols(node).sortedBy(protocolCost)
                        .firstOrNull() ?: error("protocol not found!")
                    assert(p.authority(hostTrustConfiguration).actsFor(informationFlowAnalysis.label(node)))
                    assignment = assignment.put(node.temporary.value, p)
                }
                is DeclarationNode -> {
                    // TODO: proper error class
                    val p = protocolSelection.possibleProtocols(node).sortedBy(protocolCost)
                        .firstOrNull() ?: error("protocol not found!")
                    assert(p.authority(hostTrustConfiguration).actsFor(informationFlowAnalysis.label(node)))
                    assignment = assignment.put(node.variable.value, p)
                }
            }
            node.children.forEach(::traverse)
        }
        traverse(processDeclaration)
        return assignment::getValue
    }
}
