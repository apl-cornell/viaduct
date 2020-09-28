package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.attributes.attribute
import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.analysis.TypeAnalysis
import edu.cornell.cs.apl.viaduct.protocols.ZKP
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.SpecializedProtocol
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DowngradeNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.Node
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ObjectDeclarationArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ParameterNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.types.BooleanType
import edu.cornell.cs.apl.viaduct.util.subsequences

class ZKPFactory(val program: ProgramNode) : ProtocolFactory {

    private val nameAnalysis = NameAnalysis.get(program)
    private val typeAnalysis = TypeAnalysis.get(program)

    companion object {
        private val ProgramNode.instance: List<SpecializedProtocol> by attribute {
            val hostTrustConfiguration = HostTrustConfiguration(this)
            val hosts: List<Host> = hostTrustConfiguration.keys.sorted()
            val hostSubsets = hosts.subsequences().map { it.toSet() }
            hostSubsets.filter { it.size >= 2 }.flatMap { ss ->
                ss.map { h ->
                    (h to ss.minus(h))
                }
            }.map { SpecializedProtocol(ZKP(it.first, it.second), hostTrustConfiguration) }
        }

        fun protocols(program: ProgramNode): List<SpecializedProtocol> = program.instance
    }

    override fun protocols(): List<SpecializedProtocol> = protocols(program)

    fun LetNode.onlyDeclassifyBoolean(): Boolean {
        return nameAnalysis.readers(this).all {
            when (it) {
                is LetNode ->
                    when (it.value) {
                        is DowngradeNode -> typeAnalysis.type(this) is BooleanType
                        else -> true
                    }
                else -> true
            }
        }
    }

    private fun Node.isApplicable(): Boolean =
        when (this) {
            is LetNode -> this.onlyDeclassifyBoolean()
            is DeclarationNode -> true
            is ObjectDeclarationArgumentNode -> true
            is ParameterNode -> true
            else -> false
        }

    override fun viableProtocols(node: DeclarationNode): Set<Protocol> =
        if (node.isApplicable()) {
            protocols(program).map { it.protocol }.toSet()
        } else {
            setOf()
        }

    override fun viableProtocols(node: ObjectDeclarationArgumentNode): Set<Protocol> =
        if (node.isApplicable()) {
            protocols(program).map { it.protocol }.toSet()
        } else {
            setOf()
        }

    override fun viableProtocols(node: ParameterNode): Set<Protocol> =
        if (node.isApplicable()) {
            protocols(program).map { it.protocol }.toSet()
        } else {
            setOf()
        }

    override fun viableProtocols(node: LetNode): Set<Protocol> =
        if (node.isApplicable()) {
            protocols(program).map { it.protocol }.toSet()
        } else {
            setOf()
        }

    private val localAndReplicated: Set<Protocol> =
        LocalFactory.protocols(program).map { it.protocol }.toSet() +
            ReplicationFactory.protocols(program).map { it.protocol }.toSet()

    /** ZKP can only read from, and only send to, itself, local, and replicated **/
    override fun constraint(node: LetNode): SelectionConstraint =
        protocols(program).map {
            And(node.readsFrom(nameAnalysis, setOf(it.protocol), localAndReplicated + setOf(it.protocol)),
                node.sendsTo(nameAnalysis, setOf(it.protocol), localAndReplicated + setOf(it.protocol)))
        }.ands()

    override fun constraint(node: DeclarationNode): SelectionConstraint =
        protocols(program).map {
            And(node.readsFrom(nameAnalysis, setOf(it.protocol), localAndReplicated + setOf(it.protocol)),
                node.sendsTo(nameAnalysis, setOf(it.protocol), localAndReplicated + setOf(it.protocol)))
        }.ands()
}
