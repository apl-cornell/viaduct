package io.github.apl_cornell.viaduct.selection

import io.github.apl_cornell.viaduct.analysis.InformationFlowAnalysis
import io.github.apl_cornell.viaduct.analysis.NameAnalysis
import io.github.apl_cornell.viaduct.syntax.Protocol
import io.github.apl_cornell.viaduct.syntax.intermediate.Node
import io.github.apl_cornell.viaduct.syntax.intermediate.ProgramNode
import io.github.apl_cornell.viaduct.syntax.intermediate.VariableDeclarationNode

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
    protocolAssignment: ProtocolAssignment
) {
    val constraintGenerator =
        SelectionConstraintGenerator(program, protocolFactory, protocolComposer, costEstimator)

    val nameAnalysis = NameAnalysis.get(program)
    val informationFlowAnalysis = InformationFlowAnalysis.get(program)

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
        if (!informationFlowAnalysis.trustConfiguration.actsFor(
                protocol.authority(),
                informationFlowAnalysis.label(node)
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
