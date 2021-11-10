package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.viaduct.analysis.InformationFlowAnalysis
import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.syntax.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.Node
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode

/**
 * This function provides a sanity check to ensure that a given protocol assignment satisfies all constraints
 * required on it by the selector.
 */
fun validateProtocolAssignment(
    program: ProgramNode,
    protocolFactory: ProtocolFactory,
    protocolComposer: ProtocolComposer,
    costEstimator: CostEstimator<IntegerCost>,
    protocolAssignment: ProtocolAssignment
) {
    val constraintGenerator =
        SelectionConstraintGenerator(program, protocolFactory, protocolComposer, costEstimator)

    val nameAnalysis = NameAnalysis.get(program)
    val informationFlowAnalysis = InformationFlowAnalysis.get(program)
    val hostTrustConfiguration = HostTrustConfiguration(program)

    fun checkViableProtocol(selection: ProtocolAssignment, node: LetNode) {
        val functionName = nameAnalysis.enclosingFunctionName(node)
        val protocol = selection.getAssignment(functionName, node.temporary.value)
        val l = informationFlowAnalysis.label(node)
        if (!constraintGenerator.viableProtocols(node).contains(protocol)) {
            throw error(
                "Bad protocol restriction for let node of ${node.temporary} = ${node.value}: viable protocols is ${
                constraintGenerator.viableProtocols(node).map { it.asDocument.print() }
                } but selected was ${protocol.asDocument.print()}; label is $l"
            )
        }
    }

    fun checkAuthority(selection: ProtocolAssignment, node: LetNode) {
        val functionName = nameAnalysis.enclosingFunctionName(node)
        val protocol = selection.getAssignment(functionName, node.temporary.value)
        if (!(
            protocol.authority(hostTrustConfiguration)
                .actsFor(informationFlowAnalysis.label(node))
            )
        ) {
            throw error(
                "Bad authority for let node of ${node.temporary}: protocol's authority is ${
                protocol.authority(
                    hostTrustConfiguration
                )
                }" +
                    "but node's label is ${informationFlowAnalysis.label(node)}"
            )
        }
    }

    fun checkViableProtocol(selection: ProtocolAssignment, node: DeclarationNode) {
        val functionName = nameAnalysis.enclosingFunctionName(node)
        val protocol = selection.getAssignment(functionName, node.name.value)
        if (!constraintGenerator.viableProtocols(node).contains(protocol)) {
            throw error(
                "Bad protocol restriction for decl of ${node.name}: viable protocols is ${
                constraintGenerator.viableProtocols(
                    node
                )
                } but selected was $protocol"
            )
        }
    }

    fun checkAuthority(selection: ProtocolAssignment, node: DeclarationNode) {
        val functionName = nameAnalysis.enclosingFunctionName(node)
        val protocol = selection.getAssignment(functionName, node.name.value)
        if (!(
            protocol.authority(hostTrustConfiguration)
                .actsFor(informationFlowAnalysis.label(node))
            )
        ) {
            throw error(
                "Bad authority for decl of ${node.name}: protocol's authority is ${
                protocol.authority(
                    hostTrustConfiguration
                )
                }" +
                    "but node's label is ${informationFlowAnalysis.label(node)}"
            )
        }
    }

    fun Node.traverse(selection: ProtocolAssignment) {
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

    program.traverse(protocolAssignment)

    // TODO: currently no support for host variables, so turn these off for now
    // val constraints = processDeclaration.constraints()
    // constraints.toList().assert(setOf(), protocolAssignment)
}
