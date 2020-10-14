package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.attributes.attribute
import edu.cornell.cs.apl.viaduct.analysis.InformationFlowAnalysis
import edu.cornell.cs.apl.viaduct.protocols.Local
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.SpecializedProtocol
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ObjectDeclarationArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ParameterNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode

class LocalFactory(val program: ProgramNode) : ProtocolFactory {
    private val informationFlowAnalysis = InformationFlowAnalysis.get(program)

    val protocols: List<SpecializedProtocol> = run {
        val hostTrustConfiguration = HostTrustConfiguration(program)
        val hosts: List<Host> = hostTrustConfiguration.keys.sorted()
        hosts.map(::Local).map { SpecializedProtocol(it, hostTrustConfiguration) }
    }

    override fun protocols(): List<SpecializedProtocol> = protocols

    override fun viableProtocols(node: LetNode): Set<Protocol> =
        protocols().filter {
            it.authority.actsFor(informationFlowAnalysis.label(node))
        }.map { it.protocol }.toSet()

    override fun viableProtocols(node: DeclarationNode): Set<Protocol> =
        protocols().filter {
            it.authority.actsFor(informationFlowAnalysis.label(node))
        }.map { it.protocol }.toSet()

    override fun viableProtocols(node: ParameterNode): Set<Protocol> =
        protocols().filter { it.authority.actsFor(informationFlowAnalysis.label(node)) }.map { it.protocol }.toSet()

    override fun viableProtocols(node: ObjectDeclarationArgumentNode): Set<Protocol> =
        protocols().filter { it.authority.actsFor(informationFlowAnalysis.label(node)) }.map { it.protocol }.toSet()
}
