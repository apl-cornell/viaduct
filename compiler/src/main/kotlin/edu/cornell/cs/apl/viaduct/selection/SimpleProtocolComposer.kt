package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.attributes.attribute
import edu.cornell.cs.apl.viaduct.protocols.ABY
import edu.cornell.cs.apl.viaduct.protocols.Commitment
import edu.cornell.cs.apl.viaduct.protocols.Local
import edu.cornell.cs.apl.viaduct.protocols.Replication
import edu.cornell.cs.apl.viaduct.protocols.ZKP
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutParameterInitializationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SendNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SimpleStatementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode

/** Describe how protocols should communicate / compose with each other. */
object SimpleProtocolComposer : ProtocolComposer {
    private val Pair<Protocol, Protocol>.communicate: ProtocolCommunication by attribute {
        val src: Protocol = this.first
        val dst: Protocol = this.second
        when {
            src == dst -> {
                ProtocolCommunication(
                    src.hosts.map { host ->
                        CommunicationEvent(src.internalOutputPorts[host]!!, dst.internalInputPorts[host]!!)
                    }.toSet()
                )
            }

            src is Local && dst is Local -> {
                if (src.host != dst.host) {
                    ProtocolCommunication(setOf(CommunicationEvent(src.outputPort, dst.inputPort)))
                } else {
                    ProtocolCommunication(setOf())
                }
            }

            src is Local && dst is Replication -> {
                val dstHostReceivers = dst.hosts.removeAll(src.hosts)
                val dstHostSenders = dst.hosts.removeAll(dstHostReceivers)

                // TODO: optimize this later. receivers don't necessarily need to receive from all senders
                ProtocolCommunication(
                    dstHostSenders.map { sender ->
                        CommunicationEvent(src.outputPort, dst.hostInputPorts[sender]!!)
                    }.plus(
                        src.hosts.flatMap { _ ->
                            dstHostReceivers.map { receiver ->
                                CommunicationEvent(src.outputPort, dst.hostInputPorts[receiver]!!)
                            }
                        }
                    ).toSet()
                )
            }

            src is Local && dst is ABY -> {
                ProtocolCommunication(
                    if (dst.hosts.contains(src.host)) {
                        setOf(CommunicationEvent(src.outputPort, dst.hostSecretInputPorts[src.host]!!))
                    } else {
                        // TODO: for now, assume the input is cleartext, but should compare labels
                        // to actually determine this
                        dst.hostCleartextInputPorts.values.map { inPort ->
                            CommunicationEvent(src.outputPort, inPort)
                        }.toSet()
                    }
                )
            }

            src is Local && dst is Commitment -> {
                ProtocolCommunication(
                    setOf(
                        CommunicationEvent(src.outputPort, dst.inputPort)
                    ).plus(
                        dst.hashHosts.map { hashHost ->
                            CommunicationEvent(
                                dst.createCommitmentOutputPort,
                                dst.createCommitmentInputPorts[hashHost]!!
                            )
                        }
                    )
                )
            }

            src is Replication && dst is Local -> {
                ProtocolCommunication(
                    if (src.hosts.contains(dst.host)) {
                        setOf(
                            CommunicationEvent(src.hostOutputPorts[dst.host]!!, dst.inputPort)
                        )
                    } else {
                        src.hosts.map { srcHost ->
                            CommunicationEvent(src.hostOutputPorts[srcHost]!!, dst.inputPort)
                        }.toSet()
                    }
                )
            }

            src is Replication && dst is Replication -> {
                val dstHostReceivers = dst.hosts.removeAll(src.hosts)
                val dstHostSenders = dst.hosts.removeAll(dstHostReceivers)

                // TODO: optimize this later. receivers don't necessarily need to receive from all senders
                ProtocolCommunication(
                    dstHostSenders.map { sender ->
                        CommunicationEvent(src.hostOutputPorts[sender]!!, dst.hostInputPorts[sender]!!)
                    }.plus(
                        src.hosts.flatMap { sender ->
                            dstHostReceivers.map { receiver ->
                                CommunicationEvent(src.hostOutputPorts[sender]!!, dst.hostInputPorts[receiver]!!)
                            }
                        }
                    ).toSet()
                )
            }

            src is Replication && dst is ABY -> {
                // TODO: do this properly using labels
                ProtocolCommunication(
                    when {
                        src.hosts.containsAll(dst.hosts) -> {
                            dst.hosts.map { dstHost ->
                                CommunicationEvent(
                                    src.hostOutputPorts[dstHost]!!,
                                    dst.hostCleartextInputPorts[dstHost]!!
                                )
                            }.toSet()
                        }

                        /*
                        src.hosts.intersect(dst.hosts).isNotEmpty() -> {
                            src.hosts.intersect(dst.hosts).map { host ->
                                CommunicationEvent(
                                    src.hostOutputPorts[host]!!,
                                    dst.hostSecretInputPorts[host]!!
                                )
                            }.toSet()
                        }
                        */

                        else -> {
                            src.hosts.flatMap { srcHost ->
                                dst.hosts.map { dstHost ->
                                    CommunicationEvent(
                                        src.hostOutputPorts[srcHost]!!,
                                        dst.hostCleartextInputPorts[dstHost]!!
                                    )
                                }
                            }.toSet()
                        }
                    }
                )
            }

            src is Replication && dst is Commitment -> {
                // TODO: fix this
                val cleartextEvents =
                    if (src.hosts.contains(dst.cleartextHost)) {
                        setOf(CommunicationEvent(src.hostOutputPorts[dst.cleartextHost]!!, dst.inputPort))
                    } else {
                        src.hosts.map { host ->
                            CommunicationEvent(src.hostOutputPorts[host]!!, dst.inputPort)
                        }.toSet()
                    }

                ProtocolCommunication(
                    cleartextEvents.union(
                        dst.hashHosts.map { hashHost ->
                            CommunicationEvent(
                                dst.createCommitmentOutputPort,
                                dst.createCommitmentInputPorts[hashHost]!!
                            )
                        }
                    )
                )
            }

            src is ABY && dst is Local -> {
                ProtocolCommunication(
                    if (src.hosts.contains(dst.host)) {
                        setOf(
                            CommunicationEvent(src.hostCleartextOutputPorts[dst.host]!!, dst.inputPort)
                        )
                    } else {
                        src.hosts.map { srcHost ->
                            CommunicationEvent(src.hostCleartextOutputPorts[srcHost]!!, dst.inputPort)
                        }.toSet()
                    }
                )
            }

            src is ABY && dst is Replication -> {
                val dstHostReceivers = dst.hosts.removeAll(src.hosts)
                val dstHostSenders = dst.hosts.removeAll(dstHostReceivers)

                // TODO: optimize this later. receivers don't necessarily need to receive from all senders
                ProtocolCommunication(
                    dstHostSenders.map { sender ->
                        CommunicationEvent(
                            src.hostCleartextOutputPorts[sender]!!,
                            dst.hostInputPorts[sender]!!
                        )
                    }.plus(
                        src.hosts.flatMap { sender ->
                            dstHostReceivers.map { receiver ->
                                CommunicationEvent(
                                    src.hostCleartextOutputPorts[sender]!!,
                                    dst.hostInputPorts[receiver]!!
                                )
                            }
                        }
                    ).toSet()
                )
            }

            src is ABY && dst is ABY && src.client == dst.client && src.server == dst.server -> {
                ProtocolCommunication(setOf())
            }

            src is Commitment && dst is Local -> {
                ProtocolCommunication(
                        setOf(
                            CommunicationEvent(src.openCleartextOutputPort, dst.cleartextCommitmentInputPort)
                        ).plus(
                            src.hashHosts.map { hashHost ->
                                CommunicationEvent(
                                    src.openCommitmentOutputPorts[hashHost]!!,
                                    dst.hashCommitmentInputPort
                                )
                            }.toSet()
                        )
                )
            }

            src is Commitment && dst is Replication -> {
                ProtocolCommunication(
                    dst.hosts.flatMap { host ->
                            setOf(
                                CommunicationEvent(
                                    src.openCleartextOutputPort,
                                    dst.hostCleartextCommitmentInputPorts[host]!!
                                )
                            ).plus(
                                src.hashHosts.map { hashHost ->
                                    CommunicationEvent(
                                        src.openCommitmentOutputPorts[hashHost]!!,
                                        dst.hostHashCommitmentInputPorts[host]!!
                                    )
                                }
                            )
                    }.toSet()
                )
            }

            // TODO: fix this
            src is Commitment && dst is Commitment &&
                src.cleartextHost == dst.cleartextHost && src.hashHosts == dst.hashHosts ->
            {
                ProtocolCommunication(setOf())
            }

            src is ZKP && dst is ZKP -> {
                ProtocolCommunication(setOf())
            }

            src is Local && dst is ZKP -> { // We know src.host == dst.prover
                ProtocolCommunication(setOf(CommunicationEvent(src.outputPort, dst.secretInputPort)))
            }

            src is Replication && dst is ZKP -> { // We know src.hosts == dst.verifiers + {dst.prover}
                if (src.hosts != dst.hosts) {
                    throw Exception("Bad state for composition: source hosts is ${src.hosts} but dest is ${dst.hosts}")
                }
                ProtocolCommunication(src.hosts.map {
                    CommunicationEvent(src.hostOutputPorts[it]!!, dst.cleartextInput[it]!!)
                }.toSet())
            }

            src is ZKP && dst is Local -> {
                ProtocolCommunication(src.hosts.map {
                    CommunicationEvent(src.outputPorts[it]!!, dst.inputPort)
                }.toSet())
            }

            src is ZKP && dst is Replication -> {
                ProtocolCommunication(src.hosts.flatMap { h1 ->
                    dst.hosts.map { h2 ->
                        CommunicationEvent(src.outputPorts[h1]!!, dst.hostInputPorts[h2]!!)
                    }
                }.toSet())
            }

            else -> throw Error("does not support communication from ${src.protocolName} to ${dst.protocolName}")
        }
    }

    override fun communicate(src: Protocol, dst: Protocol): ProtocolCommunication =
        Pair(src, dst).communicate

    override fun canCommunicate(src: Protocol, dst: Protocol): Boolean =
        when {
            src == dst -> true
            src is Local && dst is Local -> true
            src is Local && dst is Replication -> true
            src is Local && dst is ABY -> true
            src is Local && dst is Commitment -> true
            src is Local && dst is ZKP -> src.host == dst.prover
            src is ZKP && dst is Local -> true
            src is Replication && dst is ZKP -> src.hosts == dst.hosts // TODO generalize this?
            src is ZKP && dst is ZKP -> true
            src is ZKP && dst is Replication -> true
            src is Replication && dst is Local -> true
            src is Replication && dst is Replication -> true
            src is Replication && dst is ABY -> true
            src is Replication && dst is Commitment -> true
            src is ABY && dst is Local -> true
            src is ABY && dst is Replication -> true
            src is ABY && dst is ABY &&
                src.client == dst.client && src.server == dst.server -> true
            src is Commitment && dst is Local -> true
            src is Commitment && dst is Replication -> true
            src is Commitment && dst is Commitment &&
                src.cleartextHost == dst.cleartextHost
                && src.hashHosts == dst.hashHosts -> true
            else -> false
        }

    override fun mandatoryParticipatingHosts(protocol: Protocol, stmt: SimpleStatementNode): Set<Host> =
        when (stmt) {
            is LetNode -> {
                when (protocol) {
                    is ABY, is Local, is Commitment, is ZKP -> protocol.hosts
                    is Replication -> setOf()
                    else -> setOf()
                }
            }

            is DeclarationNode -> protocol.hosts

            is UpdateNode -> protocol.hosts

            is OutParameterInitializationNode -> protocol.hosts

            is OutputNode -> protocol.hosts

            is SendNode -> setOf()
        }

    override fun visibleGuardHosts(protocol: Protocol): Set<Host> =
        when (protocol) {
            is Local, is Replication -> protocol.hosts
            is ABY -> setOf()
            is Commitment -> setOf(protocol.cleartextHost)
            else -> setOf()
        }
}
