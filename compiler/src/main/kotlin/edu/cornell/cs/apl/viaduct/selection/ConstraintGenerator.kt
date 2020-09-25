package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.viaduct.analysis.InformationFlowAnalysis
import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.protocols.Local
import edu.cornell.cs.apl.viaduct.syntax.FunctionName
import edu.cornell.cs.apl.viaduct.syntax.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExpressionArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.Node
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ObjectDeclarationArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ObjectReferenceArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutParameterArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ParameterNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.QueryNode
import edu.cornell.cs.apl.viaduct.util.unions

class ConstraintGenerator(
    private val program: ProgramNode,
    private val protocolFactory: ProtocolFactory
) {
    private val hostTrustConfiguration = HostTrustConfiguration(program)
    private val nameAnalysis = NameAnalysis.get(program)
    private val informationFlowAnalysis = InformationFlowAnalysis.get(program)

    // TODO: pc must be weak enough for the hosts involved in the selected protocols to read it
    fun viableProtocols(node: LetNode): Set<Protocol> =
        protocolFactory.viableProtocols(node).filter {
            it.authority(hostTrustConfiguration).actsFor(informationFlowAnalysis.label(node))
        }.toSet()

    fun viableProtocols(node: DeclarationNode): Set<Protocol> =
        protocolFactory.viableProtocols(node).filter {
            it.authority(hostTrustConfiguration).actsFor(informationFlowAnalysis.label(node))
        }.toSet()

    fun viableProtocols(node: ParameterNode): Set<Protocol> =
        protocolFactory.viableProtocols(node).filter {
            it.authority(hostTrustConfiguration).actsFor(informationFlowAnalysis.label(node))
        }.toSet()

    fun viableProtocols(node: ObjectDeclarationArgumentNode): Set<Protocol> =
        protocolFactory.viableProtocols(node).filter {
            it.authority(hostTrustConfiguration).actsFor(informationFlowAnalysis.label(node))
        }.toSet()

    fun constraints(node: Node): Set<SelectionConstraint> {
        val s = when (node) {
            is LetNode ->
                setOf(
                    VariableIn(
                        Pair(nameAnalysis.enclosingFunctionName(node), node.temporary.value),
                        viableProtocols(node)
                    ),
                    protocolFactory.constraint(node)
                ) + when (node.value) {
                    is InputNode ->
                        setOf(
                            VariableIn(
                                Pair(nameAnalysis.enclosingFunctionName(node), node.temporary.value),
                                setOf(Local(node.value.host.value))
                            )
                        )
                    is QueryNode -> {
                        val decl = nameAnalysis.declaration(node.value)
                        val declFn: FunctionName = when (val n = decl.declarationAsNode) {
                            is DeclarationNode -> nameAnalysis.enclosingFunctionName(n)

                            is ParameterNode -> nameAnalysis.functionDeclaration(n).name.value

                            is ObjectDeclarationArgumentNode -> nameAnalysis.enclosingFunctionName(n)

                            else -> throw Exception("impossible")
                        }

                        setOf(
                            VariableEquals(
                                Pair(nameAnalysis.enclosingFunctionName(node), node.temporary.value),
                                Pair(declFn, decl.name.value)
                            )
                        )
                    }
                    else -> setOf()
                }

            is DeclarationNode ->
                setOf(
                    VariableIn(
                        Pair(nameAnalysis.enclosingFunctionName(node), node.name.value),
                        viableProtocols(node)
                    ),
                    protocolFactory.constraint(node)
                )

            is ObjectDeclarationArgumentNode ->
                setOf(
                    VariableIn(
                        Pair(nameAnalysis.enclosingFunctionName(node), node.name.value),
                        viableProtocols(node)
                    ),
                    protocolFactory.constraint(node)
                )

            is ExpressionArgumentNode -> {
                val parameter = nameAnalysis.parameter(node)
                val parameterFunctionName = nameAnalysis.functionDeclaration(parameter).name.value
                nameAnalysis
                    .reads(node)
                    .map { read -> nameAnalysis.declaration(read) }
                    .map { letNode ->
                        VariableEquals(
                            Pair(nameAnalysis.enclosingFunctionName(letNode), letNode.temporary.value),
                            Pair(parameterFunctionName, parameter.name.value)
                        )
                    }
                    .toSet()
            }

            is ObjectReferenceArgumentNode -> {
                val parameter = nameAnalysis.parameter(node)
                val parameterFunctionName = nameAnalysis.functionDeclaration(parameter).name.value
                setOf(
                    VariableEquals(
                        Pair(nameAnalysis.enclosingFunctionName(node), node.variable.value),
                        Pair(parameterFunctionName, parameter.name.value)
                    )
                )
            }

            is OutParameterArgumentNode -> {
                val parameter = nameAnalysis.parameter(node)
                val parameterFunctionName = nameAnalysis.functionDeclaration(parameter).name.value
                setOf(
                    VariableEquals(
                        Pair(nameAnalysis.enclosingFunctionName(node), node.parameter.value),
                        Pair(parameterFunctionName, parameter.name.value)
                    )
                )
            }

            else -> setOf()
        }

        return s.union(
            node.children.map { constraints(it) }.unions()
        )
    }
}
