package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.attributes.attribute
import edu.cornell.cs.apl.viaduct.protocols.ABY
import edu.cornell.cs.apl.viaduct.protocols.Local
import edu.cornell.cs.apl.viaduct.protocols.Replication
import edu.cornell.cs.apl.viaduct.syntax.Protocol

/** Describe how protocols should communicate / compose with each other. */
object SimpleProtocolComposer : ProtocolComposer {
    private val Pair<Protocol, Protocol>.communicate: ProtocolCommunication by attribute {
        val src: Protocol = this.first
        val dst: Protocol = this.second
        when {
            src is Local && dst is Local ->
                ProtocolCommunication(
                    setOf(CommunicationEvent(src.hostOutputPort, dst.hostInputPort))
                )

            src is Local && dst is Replication -> {
                val dstHostReceivers = dst.hosts.removeAll(src.hosts)
                val dstHostSenders = dst.hosts.removeAll(dstHostReceivers)

                // TODO: optimize this later. receivers don't necessarily need to receive from all senders
                ProtocolCommunication(
                    dstHostSenders.map { sender ->
                        CommunicationEvent(src.hostOutputPort, dst.hostInputPorts[sender]!!)
                    }.plus(
                        src.hosts.flatMap { _ ->
                            dstHostReceivers.map { receiver ->
                                CommunicationEvent(src.hostOutputPort, dst.hostInputPorts[receiver]!!)
                            }
                        }
                    ).toSet()
                )
            }

            src is Local && dst is ABY -> {
                ProtocolCommunication(
                    if (dst.hosts.contains(src.host)) {
                        val otherHost = if (dst.client == src.host) dst.server else dst.client

                        setOf(
                            CommunicationEvent(src.hostOutputPort, dst.hostSecretInputPorts[src.host]!!),
                            CommunicationEvent(dst.hostDummyOutputPorts[otherHost]!!, dst.hostDummyInputPorts[otherHost]!!)
                        )
                    } else {
                        // TODO: for now, assume the input is cleartext, but should compare labels
                        // to actually determine this
                        dst.hostCleartextInputPorts.values.map { inPort ->
                            CommunicationEvent(src.hostOutputPort, inPort)
                        }.toSet()
                    }
                )
            }

            src is Replication && dst is Local -> {
                ProtocolCommunication(
                    if (src.hosts.contains(dst.host)) {
                        setOf(
                            CommunicationEvent(src.hostOutputPorts[dst.host]!!, dst.hostInputPort)
                        )
                    } else {
                        src.hosts.map { srcHost ->
                            CommunicationEvent(src.hostOutputPorts[srcHost]!!, dst.hostInputPort)
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

            src is ABY && dst is Local -> {
                ProtocolCommunication(
                    if (src.hosts.contains(dst.host)) {
                        setOf(
                            CommunicationEvent(src.hostCleartextOutputPorts[dst.host]!!, dst.hostInputPort)
                        )
                    } else {
                        src.hosts.map { srcHost ->
                            CommunicationEvent(src.hostCleartextOutputPorts[srcHost]!!, dst.hostInputPort)
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

            // TODO: fix this later
            src is ABY && dst is ABY -> {
                val dstHostReceivers = dst.hosts.removeAll(src.hosts)
                val dstHostSenders = dst.hosts.removeAll(dstHostReceivers)

                // TODO: optimize this later. receivers don't necessarily need to receive from all senders
                ProtocolCommunication(
                    dstHostSenders.map { sender ->
                        CommunicationEvent(
                            src.hostSecretShareOutputPorts[sender]!!,
                            dst.hostSecretShareInputPorts[sender]!!
                        )
                    }.plus(
                        src.hosts.flatMap { sender ->
                            dstHostReceivers.map { receiver ->
                                CommunicationEvent(
                                    src.hostSecretShareOutputPorts[sender]!!,
                                    dst.hostSecretShareInputPorts[receiver]!!
                                )
                            }
                        }
                    ).toSet()
                )
            }

            else -> throw Error("does not support communication from ${src.protocolName} to ${dst.protocolName}")
        }
    }

    override fun communicate(src: Protocol, dst: Protocol): ProtocolCommunication =
        Pair(src, dst).communicate
}
