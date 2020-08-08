package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.viaduct.analysis.InformationFlowAnalysis
import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.errors.NoApplicableProtocolError
import edu.cornell.cs.apl.viaduct.errors.NoHostDeclarationsError
import edu.cornell.cs.apl.viaduct.protocols.Local
import edu.cornell.cs.apl.viaduct.syntax.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.Variable
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.Node
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProcessDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.QueryNode
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf

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
                viableProtocols(nameAnalysis.declaration(value))
            else ->
                protocolFactory.viableProtocols(node)
        }

    private fun viableProtocols(node: DeclarationNode): Set<Protocol> =
        protocolFactory.viableProtocols(node)

    fun select(processDeclaration: ProcessDeclarationNode): (Variable) -> Protocol {
        if (hostTrustConfiguration.isEmpty())
            throw NoHostDeclarationsError(program.sourceLocation.sourcePath)

        var constraints: SelectionConstraint = Literal(true)
        var assignment: PersistentMap<Variable, Protocol> = persistentMapOf()

        fun traverse(node: Node) {
            when (node) {
                is LetNode -> {
                    constraints = And(constraints, protocolFactory.constraint(node))
                    val p = viableProtocols(node).minBy(protocolCost) ?: throw NoApplicableProtocolError(node.temporary)
                    assignment = assignment.put(node.temporary.value, p)
                }
                is DeclarationNode -> {
                    constraints = And(constraints, protocolFactory.constraint(node))
                    val p = viableProtocols(node).minBy(protocolCost) ?: throw NoApplicableProtocolError(node.variable)
                    assignment = assignment.put(node.variable.value, p)
                }
            }
            node.children.forEach(::traverse)
        }
        traverse(processDeclaration)
        val f = assignment::getValue
        assert(constraints.evaluate(f))
        return f
    }
}
