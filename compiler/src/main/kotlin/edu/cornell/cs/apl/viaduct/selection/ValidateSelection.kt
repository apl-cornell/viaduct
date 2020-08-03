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

/** This function provides a sanity check to ensure that a given protocol selection f : Variable -> Protocol
 *  satisfies all constraints required on it by the selector.
 */

fun ValidateSelection(
    processDeclaration: ProcessDeclarationNode,
    informationFlowAnalysis: InformationFlowAnalysis,
    nameAnalysis: NameAnalysis,
    selector: ProtocolFactory,
    selection: (Variable) -> Protocol
) {
    val hostTrustConfiguration = HostTrustConfiguration(nameAnalysis.tree.root)

    val protocolSelection = object {
        private val LetNode.viableProtocols: Set<Protocol> by attribute {
            when (value) {
                is InputNode ->
                    setOf(Local(value.host.value))
                is QueryNode -> nameAnalysis.declaration(value).viableProtocols
                else ->
                    selector.viableProtocols(this)
            }
        }

        private val DeclarationNode.viableProtocols: Set<Protocol> by attribute {
            selector.viableProtocols(this)
        }

        fun viableProtocols(node: LetNode): Set<Protocol> = node.viableProtocols.filter {
            it.authority(hostTrustConfiguration).actsFor(informationFlowAnalysis.label(node))
        }.toSet()

        fun viableProtocols(node: DeclarationNode): Set<Protocol> = node.viableProtocols.filter {
            it.authority(hostTrustConfiguration).actsFor(informationFlowAnalysis.label(node))
        }.toSet()
    }

    fun checkViableProtocol(selection: (Variable) -> Protocol, node: LetNode) {
        if (!protocolSelection.viableProtocols(node).contains(selection(node.temporary.value))) {
            throw error(
                "Bad protocol restriction for let node of ${node.temporary} = ${node.value}: viable protocols is ${selector.viableProtocols(
                    node
                )} but selected was" +
                    "${selection(node.temporary.value)}"
            )
        }
    }

    fun checkAuthority(selection: (Variable) -> Protocol, node: LetNode) {
        if (!(selection(node.temporary.value).authority(hostTrustConfiguration)
                .actsFor(informationFlowAnalysis.label(node)))
        ) {
            throw error(
                "Bad authority for let node of ${node.temporary}: protocol's authority is ${selection(node.temporary.value).authority(
                    hostTrustConfiguration
                )}" +
                    "but node's label is ${informationFlowAnalysis.label(node)}"
            )
        }
    }

    fun checkViableProtocol(selection: (Variable) -> Protocol, node: DeclarationNode) {
        if (!protocolSelection.viableProtocols(node).contains(selection(node.variable.value))) {
            throw error(
                "Bad protocol restriction for decl of ${node.variable}: viable protocols is ${selector.viableProtocols(
                    node
                )} but selected was" +
                    "${selection(node.variable.value)}"
            )
        }
    }

    fun checkAuthority(selection: (Variable) -> Protocol, node: DeclarationNode) {
        if (!(selection(node.variable.value).authority(hostTrustConfiguration)
                .actsFor(informationFlowAnalysis.label(node)))
        ) {
            throw error(
                "Bad authority for decl of ${node.variable}: protocol's authority is ${selection(node.variable.value).authority(
                    hostTrustConfiguration
                )}" +
                    "but node's label is ${informationFlowAnalysis.label(node)}"
            )
        }
    }

    fun Node.traverse(selection: (Variable) -> Protocol) {
        when (this) {
            is LetNode -> {
                checkViableProtocol(selection, this)
                checkAuthority(selection, this)
            }
            is DeclarationNode -> {
                checkViableProtocol(selection, this)
                checkAuthority(selection, this)
            }
        }
        this.children.forEach {
            it.traverse(selection)
        }
    }

    fun Node.constraints(): Set<SelectionConstraint> {
        val s = when (this) {
            is LetNode ->
                setOf(
                    selector.constraint(this)
                )
            is DeclarationNode ->
                setOf(
                    selector.constraint(this)
                )
            else -> setOf()
        }

        return s.union(
            this.children.map { it.constraints() }.unions()
        )
    }

    processDeclaration.traverse(selection)
    assert(processDeclaration.constraints().toList().ands().evaluate(selection))
}
