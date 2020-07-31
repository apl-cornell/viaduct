package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.viaduct.analysis.InformationFlowAnalysis
import edu.cornell.cs.apl.viaduct.protocols.Local
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.SpecializedProtocol
import edu.cornell.cs.apl.viaduct.syntax.Variable
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode

class LocalSelector(program: ProgramNode) : ProtocolSelector {
    private val informationFlowAnalysis = InformationFlowAnalysis.get(program)

    private val protocols: List<SpecializedProtocol> = run {
        val hostTrustConfiguration = HostTrustConfiguration(program)
        val hosts: List<Host> = hostTrustConfiguration.keys.sorted()
        hosts.map(::Local).map { SpecializedProtocol(it, hostTrustConfiguration) }
    }

    override fun select(node: LetNode, currentAssignment: Map<Variable, Protocol>): Set<Protocol> {
        return protocols.filter { it.authority.actsFor(informationFlowAnalysis.label(node)) }.map { it.protocol }
            .toSet()
    }

    override fun select(node: DeclarationNode, currentAssignment: Map<Variable, Protocol>): Set<Protocol> {
        return protocols.filter { it.authority.actsFor(informationFlowAnalysis.label(node)) }.map { it.protocol }
            .toSet()
    }
}
