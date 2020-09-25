package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.attributes.attribute
import edu.cornell.cs.apl.viaduct.protocols.ABY
import edu.cornell.cs.apl.viaduct.protocols.Local
import edu.cornell.cs.apl.viaduct.protocols.Replication
import edu.cornell.cs.apl.viaduct.syntax.CommunicationEvent
import edu.cornell.cs.apl.viaduct.syntax.Protocol

/** Describe how protocols should communicate / compose with each other. */
object SimpleProtocolComposer {
    private val Pair<Protocol, Protocol>.communicate: Set<CommunicationEvent> by attribute {
        val src: Protocol = this.first
        val dst: Protocol = this.second
        when {
            src is Local && dst is Local ->
                setOf(CommunicationEvent(src.hostOutputPort, dst.hostInputPort))

            src is Local && dst is Replication -> {
                // receive in local replica and then broadcast to all other replicas
                if (dst.hosts.contains(src.host)) {
                    setOf(
                        CommunicationEvent(src.hostOutputPort, dst.hostInputPorts[src.host]!!)
                    ).plus(
                        dst.hosts
                            .filter { dstHost -> dstHost != src.host }
                            .map { dstHost ->
                                CommunicationEvent(
                                    dst.hostOutputPorts[src.host]!!,
                                    dst.hostInputPorts[dstHost]!!
                                )
                            }
                    )
                } else {
                    dst.hosts.map { dstHost ->
                        CommunicationEvent(src.hostOutputPort, dst.hostInputPorts[dstHost]!!)
                    }.toSet()
                }
            }

            src is Local && dst is ABY -> {
                if (dst.hosts.contains(src.host)) {
                    setOf(
                        CommunicationEvent(src.hostOutputPort, dst.hostSecretInputPorts[src.host]!!)
                    )
                } else {
                    // TODO: for now, assume the input is cleartext, but should compare labels
                    // to actually determine this
                    dst.hostCleartextInputPorts.values.map { inPort ->
                        CommunicationEvent(src.hostOutputPort, inPort)
                    }.toSet()
                }
            }

            src is Replication && dst is Local -> {
                if (src.hosts.contains(dst.host)) {
                    setOf(
                        CommunicationEvent(src.hostOutputPorts[dst.host]!!, dst.hostInputPort)
                    )
                } else {
                    setOf()
                }
            }

            src is Replication && dst is Replication -> {
                val dstHostReceivers = dst.hosts.removeAll(src.hosts)
                val dstHostSenders = dst.hosts.removeAll(dstHostReceivers)

                // TODO: optimize this later. receivers don't necessarily need to receive from all senders
                dstHostSenders.map { sender ->
                    CommunicationEvent(src.hostOutputPorts[sender]!!, dst.hostInputPorts[sender]!!)
                }.plus(
                    src.hosts.flatMap { sender ->
                        dstHostReceivers.map { receiver ->
                            CommunicationEvent(src.hostOutputPorts[sender]!!, dst.hostInputPorts[receiver]!!)
                        }
                    }
                ).toSet()
            }

            src is Replication && dst is ABY -> {
                // TODO: do this properly using labels
                when {
                    src.hosts.containsAll(dst.hosts) -> {
                        dst.hosts.map { dstHost ->
                            CommunicationEvent(
                                src.hostOutputPorts[dstHost]!!,
                                dst.hostCleartextInputPorts[dstHost]!!
                            )
                        }.toSet()
                    }

                    src.hosts.intersect(dst.hosts).isNotEmpty() -> {
                        src.hosts.intersect(dst.hosts).map { host ->
                            CommunicationEvent(
                                src.hostOutputPorts[host]!!,
                                dst.hostSecretInputPorts[host]!!
                            )
                        }.toSet()
                    }

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
            }

            src is ABY && dst is Local -> {
                if (src.hosts.contains(dst.host)) {
                    setOf(
                        CommunicationEvent(src.hostCleartextOutputPorts[dst.host]!!, dst.hostInputPort)
                    )
                } else {
                    src.hosts.map { srcHost ->
                        CommunicationEvent(src.hostCleartextOutputPorts[srcHost]!!, dst.hostInputPort)
                    }.toSet()
                }
            }

            src is ABY && dst is Replication -> {
                val dstHostReceivers = dst.hosts.removeAll(src.hosts)
                val dstHostSenders = dst.hosts.removeAll(dstHostReceivers)

                // TODO: optimize this later. receivers don't necessarily need to receive from all senders
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
            }

            // TODO: fix this later
            src is ABY && dst is ABY -> {
                if (src.hosts == dst.hosts) {
                    setOf()
                } else {
                    src.hosts.flatMap { srcHost ->
                        dst.hosts.map { dstHost ->
                            CommunicationEvent(
                                src.hostCleartextOutputPorts[srcHost]!!,
                                dst.hostCleartextInputPorts[dstHost]!!
                            )
                        }
                    }.toSet()
                }
            }

            else -> throw Error("does not support communication from ${src.protocolName} to ${dst.protocolName}")
        }
    }

    fun communicate(src: Protocol, dst: Protocol): Set<CommunicationEvent> =
        Pair(src, dst).communicate
}
