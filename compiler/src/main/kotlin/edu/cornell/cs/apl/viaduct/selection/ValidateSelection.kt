package edu.cornell.cs.apl.viaduct.selection

import com.microsoft.z3.Context
import edu.cornell.cs.apl.viaduct.analysis.InformationFlowAnalysis
import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.syntax.FunctionName
import edu.cornell.cs.apl.viaduct.syntax.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.Variable
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.Node
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProcessDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.util.unions

/** This function provides a sanity check to ensure that a given protocol selection f : Variable -> Protocol
 *  satisfies all constraints required on it by the selector.
 */

fun validateProtocolAssignment(
    program: ProgramNode,
    processDeclaration: ProcessDeclarationNode,
    protocolFactory: ProtocolFactory,
    protocolComposer: ProtocolComposer,
    costEstimator: CostEstimator<IntegerCost>,
    protocolAssignment: (FunctionName, Variable) -> Protocol
) {

    val ctx = Context()

    val constraintGenerator =
        SelectionConstraintGenerator(program, protocolFactory, protocolComposer, costEstimator, ctx)

    val nameAnalysis = NameAnalysis.get(program)
    val informationFlowAnalysis = InformationFlowAnalysis.get(program)
    val hostTrustConfiguration = HostTrustConfiguration(program)

    fun checkViableProtocol(selection: (FunctionName, Variable) -> Protocol, node: LetNode) {
        val functionName = nameAnalysis.enclosingFunctionName(node)
        val protocol = selection(functionName, node.temporary.value)
        val l = informationFlowAnalysis.label(node)
        if (!constraintGenerator.viableProtocols(node).contains(protocol)) {
            throw error(
                "Bad protocol restriction for let node of ${node.temporary} = ${node.value}: viable protocols is ${
                    constraintGenerator.viableProtocols(
                        node
                    )
                } but selected was $protocol; label is $l"
            )
        }
    }

    fun checkAuthority(selection: (FunctionName, Variable) -> Protocol, node: LetNode) {
        val functionName = nameAnalysis.enclosingFunctionName(node)
        val protocol = selection(functionName, node.temporary.value)
        if (!(protocol.authority(hostTrustConfiguration)
                .actsFor(informationFlowAnalysis.label(node)))
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

    fun checkViableProtocol(selection: (FunctionName, Variable) -> Protocol, node: DeclarationNode) {
        val functionName = nameAnalysis.enclosingFunctionName(node)
        val protocol = selection(functionName, node.name.value)
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

    fun checkAuthority(selection: (FunctionName, Variable) -> Protocol, node: DeclarationNode) {
        val functionName = nameAnalysis.enclosingFunctionName(node)
        val protocol = selection(functionName, node.name.value)
        if (!(protocol.authority(hostTrustConfiguration)
                .actsFor(informationFlowAnalysis.label(node)))
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

    fun Node.traverse(selection: (FunctionName, Variable) -> Protocol) {
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

    fun Node.constraints(): Set<SelectionConstraint> =
        constraintGenerator.getConstraints(this).union(
            this.children.map { it.constraints() }.unions()
        )

    processDeclaration.traverse(protocolAssignment)

    // TODO: currently no support for host variables, so turn these off for now
    // val constraints = processDeclaration.constraints()
    // constraints.toList().assert(setOf(), protocolAssignment)

    ctx.close()
}
