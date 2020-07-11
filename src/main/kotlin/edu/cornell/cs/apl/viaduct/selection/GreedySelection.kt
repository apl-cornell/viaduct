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

class GreedySelection(val selector: ProtocolSelector, val protocolSort: (Protocol) -> Int) {
    fun select(
        processDeclaration: ProcessDeclarationNode,
        nameAnalysis: NameAnalysis,
        informationFlowAnalysis: InformationFlowAnalysis
    ): (Variable) -> Protocol {
        val hostTrustConfiguration = HostTrustConfiguration(nameAnalysis.tree.root)
        var assignment: MutableMap<Variable, Protocol> = mutableMapOf()
        val protocolSelection = object {
            private val LetNode.possibleProtocols: Set<Protocol> by attribute {
                when (value) {
                    is InputNode ->
                        setOf(Local(value.host.value))
                    is QueryNode -> nameAnalysis.declaration(value).possibleProtocols
                    else ->
                        selector.selectLet(assignment, this)
                }
            }

            private val DeclarationNode.possibleProtocols: Set<Protocol> by attribute {
                selector.selectDeclaration(assignment, this)
            }

            fun possibleProtocols(node: LetNode) = node.possibleProtocols

            fun possibleProtocols(node: DeclarationNode) = node.possibleProtocols
        }

        fun traverse(node: Node) {
            when (node) {
                is LetNode -> {
                    val p = protocolSelection.possibleProtocols(node).sortedBy(protocolSort)
                        .firstOrNull() ?: error("protocol not found!")
                    assert(p.authority(hostTrustConfiguration).actsFor(informationFlowAnalysis.label(node)))
                    assignment.put(node.temporary.value, p)
                }
                is DeclarationNode -> {
                    val p = protocolSelection.possibleProtocols(node).sortedBy(protocolSort)
                        .firstOrNull() ?: error("protocol not found!")
                    assert(p.authority(hostTrustConfiguration).actsFor(informationFlowAnalysis.label(node)))
                    assignment.put(node.variable.value, p)
                }
            }
            node.children.forEach(::traverse)
        }
        traverse(processDeclaration)
        return assignment::getValue
    }
}
