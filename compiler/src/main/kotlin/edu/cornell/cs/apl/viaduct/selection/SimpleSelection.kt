package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.viaduct.analysis.InformationFlowAnalysis
import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.analysis.main
import edu.cornell.cs.apl.viaduct.errors.NoApplicableProtocolError
import edu.cornell.cs.apl.viaduct.errors.NoHostDeclarationsError
import edu.cornell.cs.apl.viaduct.protocols.Local
import edu.cornell.cs.apl.viaduct.syntax.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.Variable
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
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf

/**
 * This class implements a particularly simple but ineffective protocol selection.
 * Along with a protocol selector, it takes as input a function [protocolCost] which
 * gives a total linear order on protocol cost.
 */
class SimpleSelection(
    private val program: ProgramNode,
    private val protocolFactory: ProtocolFactory,
    private val protocolCost: (Protocol) -> Int
) {
    private val nameAnalysis = NameAnalysis.get(program)
    private val informationFlowAnalysis = InformationFlowAnalysis.get(program)
    private val hostTrustConfiguration = HostTrustConfiguration(program)

    private fun viableProtocols(node: LetNode): Set<Protocol> =
        when (val value = node.value) {
            is InputNode ->
                setOf(Local(value.host.value))
            is QueryNode ->
                when (val declaration = nameAnalysis.declaration(value).declarationAsNode) {
                    is DeclarationNode -> {
                        viableProtocols(declaration)
                    }

                    is ParameterNode -> {
                        viableProtocols(declaration)
                    }

                    is ObjectDeclarationArgumentNode -> {
                        viableProtocols(nameAnalysis.parameter(declaration))
                    }

                    else -> throw Exception("impossible case")
                }
            else ->
                protocolFactory.viableProtocols(node)
        }

    private fun viableProtocols(node: DeclarationNode): Set<Protocol> =
        protocolFactory.viableProtocols(node)

    private fun viableProtocols(node: ParameterNode): Set<Protocol> =
        protocolFactory.viableProtocols(node)

    fun select(program: ProgramNode): (Variable) -> Protocol {
        if (hostTrustConfiguration.isEmpty()) {
            throw NoHostDeclarationsError(program.sourceLocation.sourcePath)
        }

        var constraints: SelectionConstraint = Literal(true)
        var assignment: PersistentMap<Variable, Protocol> = persistentMapOf()
        fun traverse(node: Node) {
            when (node) {
                is LetNode -> {
                    if (!assignment.containsKey(node.temporary.value)) {
                        constraints = And(constraints, protocolFactory.constraint(node))
                        val p =
                            viableProtocols(node).minBy(protocolCost) ?: throw NoApplicableProtocolError(node.temporary)
                        assignment = assignment.put(node.temporary.value, p)
                    }
                }
                is DeclarationNode -> {
                    if (!assignment.containsKey(node.name.value)) {
                        constraints = And(constraints, protocolFactory.constraint(node))
                        val p =
                            viableProtocols(node).minBy(protocolCost) ?: throw NoApplicableProtocolError(node.name)
                        assignment = assignment.put(node.name.value, p)
                    }
                }
            }
            node.children.forEach(::traverse)
        }

        // select for parameters first; given f(x) and P(n) for the protocol for node n,
        // the following scenarios induce equality constraints on protocol selection:
        //
        // (1) fun g(y) { f(y) } => P(x) = P(y)
        // (2) fun f(x) { h(x) } => P(x) = P(z) where fun h(z) { ... }
        // (3) let a = ...; f(a) => P(x) = P(a)
        // (4) f(&obj)           => P(x) = P(obj)
        //
        // we induce these equality constraints by intersecting candidate protocol sets together
        for (function in program.functions) {
            for (parameter in function.parameters) {
                if (!assignment.containsKey(parameter.name.value)) {
                    // the set of variables that need to have the same protocol as the parameter
                    val correlatedVariables = mutableSetOf<Variable>()

                    val candidateProtocols = mutableSetOf<Protocol>()
                    candidateProtocols.addAll(viableProtocols(parameter))

                    // case (2) above
                    for (parameterUseSite in nameAnalysis.parameterUses(parameter)) {
                        correlatedVariables.add(parameterUseSite.name.value)
                        candidateProtocols.intersect(viableProtocols(parameterUseSite))
                    }

                    for (user in nameAnalysis.parameterUsers(parameter)) {
                        candidateProtocols.intersect(
                            when (user) {
                                // case (3) above
                                is ExpressionArgumentNode ->
                                    nameAnalysis.reads(user).fold(persistentSetOf()) { acc2, read ->
                                        val letNode = nameAnalysis.declaration(read)
                                        correlatedVariables.add(letNode.temporary.value)
                                        acc2.addAll(viableProtocols(letNode))
                                    }

                                // case (4) above
                                is ObjectReferenceArgumentNode -> {
                                    when (val decl = nameAnalysis.declaration(user).declarationAsNode) {
                                        is DeclarationNode -> {
                                            correlatedVariables.add(decl.name.value)
                                            viableProtocols(decl)
                                        }

                                        is ParameterNode -> {
                                            correlatedVariables.add(decl.name.value)
                                            viableProtocols(decl)
                                        }

                                        else -> persistentSetOf()
                                    }
                                }

                                is ObjectDeclarationArgumentNode ->
                                    persistentSetOf()

                                // case (1) above
                                is OutParameterArgumentNode -> {
                                    val outParameter = nameAnalysis.declaration(user)
                                    correlatedVariables.add(outParameter.name.value)
                                    viableProtocols(outParameter)
                                }
                            }
                        )
                    }

                    val p =
                        candidateProtocols.minBy(protocolCost)
                        ?: throw NoApplicableProtocolError(parameter.name)

                    assert(p.authority(hostTrustConfiguration).actsFor(informationFlowAnalysis.label(parameter)))

                    assignment =
                        correlatedVariables
                            .fold(assignment.put(parameter.name.value, p)) { acc, v -> acc.put(v, p) }
                }
            }
        }

        // select for function bodies
        for (function in program.functions) {
            traverse(function.body)
        }

        // finally, select for main process
        traverse(program.main)

        val f = assignment::getValue
        assert(constraints.evaluate(f))
        return f
    }
}
