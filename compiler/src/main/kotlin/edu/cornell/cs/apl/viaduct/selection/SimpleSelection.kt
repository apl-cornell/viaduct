package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.viaduct.analysis.InformationFlowAnalysis
import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
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

private typealias PartialAssignment = PersistentMap<Variable, Protocol>

/**
 * This class implements a particularly simple but ineffective protocol selection.
 * Along with a protocol selector, it takes as input a function [protocolCost] which
 * gives a total linear order on protocol cost.
 */
class SimpleSelection(
    private val program: ProgramNode,
    private val selector: ProtocolSelector,
    private val protocolCost: (Protocol) -> Int
) {
    private val nameAnalysis = NameAnalysis.get(program)
    private val informationFlowAnalysis = InformationFlowAnalysis.get(program)
    private val hostTrustConfiguration = HostTrustConfiguration(program)

    private fun possibleProtocols(node: LetNode, assignment: PartialAssignment): Set<Protocol> =
        when (val value = node.value) {
            is InputNode ->
                setOf(Local(value.host.value))
            is QueryNode ->
                possibleProtocols(nameAnalysis.declaration(value), assignment)
            else ->
                selector.select(node, assignment)
        }

    private fun possibleProtocols(node: DeclarationNode, assignment: PartialAssignment): Set<Protocol> =
        selector.select(node, assignment)

    fun select(processDeclaration: ProcessDeclarationNode): (Variable) -> Protocol {
        if (hostTrustConfiguration.isEmpty())
            throw NoHostDeclarationsError(program.sourceLocation.sourcePath)

        var assignment: PersistentMap<Variable, Protocol> = persistentMapOf()

        fun traverse(node: Node) {
            when (node) {
                is LetNode -> {
                    // TODO: proper error class
                    val p = possibleProtocols(node, assignment).minBy(protocolCost) ?: error("protocol not found!")
                    assert(p.authority(hostTrustConfiguration).actsFor(informationFlowAnalysis.label(node)))
                    assignment = assignment.put(node.temporary.value, p)
                }
                is DeclarationNode -> {
                    // TODO: proper error class
                    val p = possibleProtocols(node, assignment).minBy(protocolCost) ?: error("protocol not found!")
                    assert(p.authority(hostTrustConfiguration).actsFor(informationFlowAnalysis.label(node)))
                    assignment = assignment.put(node.variable.value, p)
                }
            }
            node.children.forEach(::traverse)
        }
        traverse(processDeclaration)
        return assignment::getValue
    }
}
