package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.viaduct.analysis.InformationFlowAnalysis
import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.syntax.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.Node
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode

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
    val hostTrustConfiguration = HostTrustConfiguration(program)

    fun checkViableProtocol(selection: ProtocolAssignment, node: LetNode) {
        val functionName = nameAnalysis.enclosingFunctionName(node)
        val protocol = selection.getAssignment(functionName, node.temporary.value)
        if (!constraintGenerator.viableProtocols(node).contains(protocol)) {
            throw InvalidProtocolAssignmentException(node, protocol)
        }
    }

    fun checkAuthority(selection: ProtocolAssignment, node: LetNode) {
        val functionName = nameAnalysis.enclosingFunctionName(node)
        val protocol = selection.getAssignment(functionName, node.temporary.value)
        if (!protocol.authority(hostTrustConfiguration).actsFor(informationFlowAnalysis.label(node))) {
            throw InvalidProtocolAssignmentException(node, protocol)
        }
    }

    fun checkViableProtocol(selection: ProtocolAssignment, node: DeclarationNode) {
        val functionName = nameAnalysis.enclosingFunctionName(node)
        val protocol = selection.getAssignment(functionName, node.name.value)
        if (!constraintGenerator.viableProtocols(node).contains(protocol)) {
            throw InvalidProtocolAssignmentException(node, protocol)
        }
    }

    fun checkAuthority(selection: ProtocolAssignment, node: DeclarationNode) {
        val functionName = nameAnalysis.enclosingFunctionName(node)
        val protocol = selection.getAssignment(functionName, node.name.value)
        if (!protocol.authority(hostTrustConfiguration).actsFor(informationFlowAnalysis.label(node))) {
            throw InvalidProtocolAssignmentException(node, protocol)
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

class InvalidProtocolAssignmentException(node: Node, protocol: Protocol) :
    RuntimeException("Protocol ${protocol.name} is invalid for ${node.toDocument().print()}.")
