package io.github.aplcornell.viaduct.selection

import io.github.aplcornell.viaduct.analysis.HostTrustConfiguration
import io.github.aplcornell.viaduct.analysis.InformationFlowAnalysis
import io.github.aplcornell.viaduct.analysis.NameAnalysis
import io.github.aplcornell.viaduct.syntax.Protocol
import io.github.aplcornell.viaduct.syntax.intermediate.Node
import io.github.aplcornell.viaduct.syntax.intermediate.ProgramNode
import io.github.aplcornell.viaduct.syntax.intermediate.VariableDeclarationNode

/**
 * This function provides a sanity check to ensure that a given protocol assignment
 * satisfies all constraints required on it by the selector.
 *
 * @throws InvalidProtocolAssignmentException if [protocolAssignment] is invalid for [program].
 */
fun validateProtocolAssignment(
    program: ProgramNode,
    protocolFactory: ProtocolFactory,
    protocolComposer: ProtocolComposer,
    costEstimator: CostEstimator<IntegerCost>,
    protocolAssignment: ProtocolAssignment,
) {
    val constraintGenerator =
        SelectionConstraintGenerator(program, protocolFactory, protocolComposer, costEstimator)

    val hostTrustConfiguration = program.analyses.get<HostTrustConfiguration>()
    val nameAnalysis = program.analyses.get<NameAnalysis>()
    val informationFlowAnalysis = program.analyses.get<InformationFlowAnalysis>()

    fun checkViableProtocol(selection: ProtocolAssignment, node: VariableDeclarationNode) {
        val functionName = nameAnalysis.enclosingFunctionName(node as Node)
        val protocol = selection.getAssignment(functionName, node.name.value)
        if (!constraintGenerator.viableProtocols(node).contains(protocol)) {
            throw InvalidProtocolAssignmentException(node, protocol)
        }
    }

    fun checkAuthority(selection: ProtocolAssignment, node: VariableDeclarationNode) {
        val functionName = nameAnalysis.enclosingFunctionName(node as Node)
        val protocol = selection.getAssignment(functionName, node.name.value)
        if (!hostTrustConfiguration.actsFor(
                protocol.authority(),
                informationFlowAnalysis.label(node),
            )
        ) {
            // if (!protocol.authority().actsFor(informationFlowAnalysis.label(node))) {
            throw InvalidProtocolAssignmentException(node, protocol)
        }
    }

    fun Node.traverse(selection: ProtocolAssignment) {
        when (this) {
            is VariableDeclarationNode -> {
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

class InvalidProtocolAssignmentException(node: Node, protocol: Protocol) :
    RuntimeException("Protocol ${protocol.name} is invalid for ${node.toDocument().print()}.")
