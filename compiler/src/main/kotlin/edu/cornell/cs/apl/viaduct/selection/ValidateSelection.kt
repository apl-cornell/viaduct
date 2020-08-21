package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.attributes.attribute
import edu.cornell.cs.apl.viaduct.analysis.InformationFlowAnalysis
import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.protocols.Local
import edu.cornell.cs.apl.viaduct.syntax.FunctionName
import edu.cornell.cs.apl.viaduct.syntax.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.Variable
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.Node
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ObjectDeclarationArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ParameterNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProcessDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.QueryNode
import edu.cornell.cs.apl.viaduct.util.unions

/** This function provides a sanity check to ensure that a given protocol selection f : Variable -> Protocol
 *  satisfies all constraints required on it by the selector.
 */

fun validateProtocolAssignment(
    program: ProgramNode,
    processDeclaration: ProcessDeclarationNode,
    protocolFactory: ProtocolFactory,
    protocolAssignment: (FunctionName, Variable) -> Protocol
) {
    val nameAnalysis = NameAnalysis.get(program)
    val informationFlowAnalysis = InformationFlowAnalysis.get(program)
    val hostTrustConfiguration = HostTrustConfiguration(program)

    val protocolSelection = object {
        private val LetNode.viableProtocols: Set<Protocol> by attribute {
            when (value) {
                is InputNode ->
                    setOf(Local(value.host.value))
                is QueryNode ->
                    when (val declaration = nameAnalysis.declaration(value).declarationAsNode) {
                        is DeclarationNode -> declaration.viableProtocols

                        is ParameterNode -> declaration.viableProtocols

                        is ObjectDeclarationArgumentNode ->
                            nameAnalysis.parameter(declaration).viableProtocols

                        // TODO: add better exception type
                        else -> throw Exception("impossible case")
                    }

                else ->
                    protocolFactory.viableProtocols(this)
            }
        }

        private val DeclarationNode.viableProtocols: Set<Protocol> by attribute {
            protocolFactory.viableProtocols(this)
        }

        private val ParameterNode.viableProtocols: Set<Protocol> by attribute {
            protocolFactory.viableProtocols(this)
        }

        fun viableProtocols(node: LetNode): Set<Protocol> = node.viableProtocols.filter {
            it.authority(hostTrustConfiguration).actsFor(informationFlowAnalysis.label(node))
        }.toSet()

        fun viableProtocols(node: DeclarationNode): Set<Protocol> = node.viableProtocols.filter {
            it.authority(hostTrustConfiguration).actsFor(informationFlowAnalysis.label(node))
        }.toSet()
    }

    fun checkViableProtocol(selection: (FunctionName, Variable) -> Protocol, node: LetNode) {
        val functionName = nameAnalysis.enclosingFunctionName(node)
        val protocol = selection(functionName, node.temporary.value)
        if (!protocolSelection.viableProtocols(node).contains(protocol)) {
            throw error(
                "Bad protocol restriction for let node of ${node.temporary} = ${node.value}: viable protocols is ${protocolFactory.viableProtocols(
                    node
                )} but selected was $protocol"
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
                "Bad authority for let node of ${node.temporary}: protocol's authority is ${protocol.authority(
                    hostTrustConfiguration
                )}" +
                    "but node's label is ${informationFlowAnalysis.label(node)}"
            )
        }
    }

    fun checkViableProtocol(selection: (FunctionName, Variable) -> Protocol, node: DeclarationNode) {
        val functionName = nameAnalysis.enclosingFunctionName(node)
        val protocol = selection(functionName, node.name.value)
        if (!protocolSelection.viableProtocols(node).contains(protocol)) {
            throw error(
                "Bad protocol restriction for decl of ${node.name}: viable protocols is ${protocolFactory.viableProtocols(
                    node
                )} but selected was $protocol"
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
                "Bad authority for decl of ${node.name}: protocol's authority is ${protocol.authority(
                    hostTrustConfiguration
                )}" +
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

    fun Node.constraints(): Set<SelectionConstraint> {
        val s = when (this) {
            is LetNode ->
                setOf(
                    protocolFactory.constraint(this)
                )
            is DeclarationNode ->
                setOf(
                    protocolFactory.constraint(this)
                )
            else -> setOf()
        }

        return s.union(
            this.children.map { it.constraints() }.unions()
        )
    }

    processDeclaration.traverse(protocolAssignment)
    assert(processDeclaration.constraints().toList().ands().evaluate(protocolAssignment))
}
