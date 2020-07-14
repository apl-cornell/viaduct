package edu.cornell.cs.apl.viaduct.protocols

import edu.cornell.cs.apl.viaduct.analysis.InformationFlowAnalysis
import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.selection.ProtocolSelector
import edu.cornell.cs.apl.viaduct.syntax.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.Variable
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode

class SimpleSelector(nameAnalysis: NameAnalysis, val informationFlowAnalysis: InformationFlowAnalysis) :
    ProtocolSelector {
    private val hostTrustConfiguration = HostTrustConfiguration(nameAnalysis.tree.root)
    private val mpcSelector = ABYSelector(nameAnalysis, hostTrustConfiguration, informationFlowAnalysis)
    private val replicatedSelector = ReplicationSelector(hostTrustConfiguration, informationFlowAnalysis)
    private val localSelector = LocalSelector(hostTrustConfiguration, informationFlowAnalysis)

    override fun selectLet(assignment: Map<Variable, Protocol>, node: LetNode): Set<Protocol> {
        return mpcSelector.selectLet(assignment, node).union(
            replicatedSelector.selectLet(assignment, node).union(
                localSelector.selectLet(assignment, node)
            )
        )
    }

    override fun selectDeclaration(assignment: Map<Variable, Protocol>, node: DeclarationNode): Set<Protocol> {
        return mpcSelector.selectDeclaration(assignment, node).union(
            replicatedSelector.selectDeclaration(assignment, node).union(
                localSelector.selectDeclaration(assignment, node)
            )
        )
    }
}

fun simpleProtocolSort(p: Protocol): Int {
    return when (p) {
        is Local -> 0
        is Replication -> 1
        is ABY -> 2
        else -> 10
    }
}
