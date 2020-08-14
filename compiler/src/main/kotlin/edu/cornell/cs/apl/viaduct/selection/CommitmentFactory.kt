package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.attributes.attribute
import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.analysis.immediateRHS
import edu.cornell.cs.apl.viaduct.analysis.involvedVariables
import edu.cornell.cs.apl.viaduct.protocols.Commitment
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.SpecializedProtocol
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AtomicExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DowngradeNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.Node
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.QueryNode
import edu.cornell.cs.apl.viaduct.util.subsequences

class CommitmentFactory(val program: ProgramNode) : ProtocolFactory {

    private val nameAnalysis = NameAnalysis.get(program)

    companion object {
        private val ProgramNode.instance: List<SpecializedProtocol> by attribute {
            val hostTrustConfiguration = HostTrustConfiguration(this)
            val hosts: List<Host> = hostTrustConfiguration.keys.sorted()
            val hostSubsets = hosts.subsequences().map { it.toSet() }
            hostSubsets.filter { it.size >= 2 }.flatMap { ss ->
                ss.map { h ->
                    (h to ss.minus(h))
                }
            }.map { SpecializedProtocol(Commitment(it.first, it.second), hostTrustConfiguration) }
        }

        fun protocols(program: ProgramNode): List<SpecializedProtocol> = program.instance
    }

    private fun Node.isApplicable(): Boolean {
        return when (this) {
            is LetNode -> nameAnalysis.readers(this).all {
                it.immediateRHS().all {
                    (it is AtomicExpressionNode) || (it is DowngradeNode)
                }
            }
            is DeclarationNode -> nameAnalysis.users(this).all {
                it is QueryNode
            }
            else -> false
        }
    }

    override fun viableProtocols(node: DeclarationNode): Set<Protocol> {
        return if (node.isApplicable()) {
            protocols(program).map { it.protocol }.toSet()
        } else {
            setOf()
        }
    }

    override fun viableProtocols(node: LetNode): Set<Protocol> {
        return if (node.isApplicable()) {
            protocols(program).map { it.protocol }.toSet()
        } else {
            setOf()
        }
    }

    /** Selection constraint for commitment.
     *  If a let node is selected for commitment, then readers can only be local or replicated
     */

    fun readersIn(node: LetNode, pset: Set<Protocol>): SelectionConstraint {
        return nameAnalysis.readers(node).flatMap {
            it.immediateRHS().flatMap {
                it.involvedVariables().map {
                    VariableIn(it, pset)
                }
            }
        }.ands()
    }

    override fun constraint(node: LetNode): SelectionConstraint {
        return protocols(program).map {
            Implies(
                VariableIn(node.temporary.value, setOf(it.protocol)),
                readersIn(node,
                    setOf(it.protocol) + LocalFactory.protocols(program).map { it.protocol }.toSet() +
                        ReplicationFactory.protocols(program).map { it.protocol }.toSet()
                )
            )
        }.ands()
    }

    private fun usersIn(node: DeclarationNode, pset: Set<Protocol>): SelectionConstraint {
        return nameAnalysis.users(node).flatMap { user ->
            when (user) {
                is QueryNode -> user.involvedVariables().map {
                    VariableIn(it, pset)
                }
                else -> listOf(Literal(true))
            }
        }.ands()
    }

    override fun constraint(node: DeclarationNode): SelectionConstraint {
        return protocols(program).map {
            Implies(
                VariableIn(node.variable.value, setOf(it.protocol)),
                usersIn(node,
                    setOf(it.protocol) + LocalFactory.protocols(program).map { it.protocol }.toSet() +
                        ReplicationFactory.protocols(program).map { it.protocol }.toSet()
                )
            )
        }.ands()
    }
}
