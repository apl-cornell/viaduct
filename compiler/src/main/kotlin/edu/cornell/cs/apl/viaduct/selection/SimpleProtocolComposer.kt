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
                ProtocolCommunication(mapOf(
                    "send" to setOf(CommunicationEvent(src.hostOutputPort, dst.hostInputPort)),
                    "broadcast" to setOf()
                ))

            src is Local && dst is Replication -> {
                val dstHostReceivers = dst.hosts.removeAll(src.hosts)
                val dstHostSenders = dst.hosts.removeAll(dstHostReceivers)

                // TODO: optimize this later. receivers don't necessarily need to receive from all senders
                ProtocolCommunication(mapOf(
                    "send" to
                        dstHostSenders.map { sender ->
                            CommunicationEvent(src.hostOutputPort, dst.hostInputPorts[sender]!!)
                        }.plus(
                            src.hosts.flatMap { _ ->
                                dstHostReceivers.map { receiver ->
                                    CommunicationEvent(src.hostOutputPort, dst.hostInputPorts[receiver]!!)
                                }
                            }
                        ).toSet(),

                    "broadcast" to setOf()
                ))
            }

            src is Local && dst is ABY -> {
                ProtocolCommunication(mapOf(
                    "send" to
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
                        },

                    "broadcast" to setOf()
                ))
            }

            src is Replication && dst is Local -> {
                ProtocolCommunication(mapOf(
                    "send" to
                        if (src.hosts.contains(dst.host)) {
                            setOf(
                                CommunicationEvent(src.hostOutputPorts[dst.host]!!, dst.hostInputPort)
                            )
                        } else {
                            src.hosts.map { srcHost ->
                                CommunicationEvent(src.hostOutputPorts[srcHost]!!, dst.hostInputPort)
                            }.toSet()
                        },

                    "broadcast" to setOf()
                ))
            }

            src is Replication && dst is Replication -> {
                val dstHostReceivers = dst.hosts.removeAll(src.hosts)
                val dstHostSenders = dst.hosts.removeAll(dstHostReceivers)

                // TODO: optimize this later. receivers don't necessarily need to receive from all senders
                ProtocolCommunication(mapOf(
                    "send" to
                        dstHostSenders.map { sender ->
                            CommunicationEvent(src.hostOutputPorts[sender]!!, dst.hostInputPorts[sender]!!)
                        }.plus(
                            src.hosts.flatMap { sender ->
                                dstHostReceivers.map { receiver ->
                                    CommunicationEvent(src.hostOutputPorts[sender]!!, dst.hostInputPorts[receiver]!!)
                                }
                            }
                        ).toSet(),

                    "broadcast" to setOf()
                ))
            }

            src is Replication && dst is ABY -> {
                // TODO: do this properly using labels
                ProtocolCommunication(mapOf(
                    "send" to
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
                        },

                    "broadcast" to setOf()
                ))
            }

            src is ABY && dst is Local -> {
                ProtocolCommunication(mapOf(
                    "send" to
                        if (src.hosts.contains(dst.host)) {
                            setOf(
                                CommunicationEvent(src.hostCleartextOutputPorts[dst.host]!!, dst.hostInputPort)
                            )
                        } else {
                            src.hosts.map { srcHost ->
                                CommunicationEvent(src.hostCleartextOutputPorts[srcHost]!!, dst.hostInputPort)
                            }.toSet()
                        },

                    "broadcast" to setOf()
                ))
            }

            src is ABY && dst is Replication -> {
                val dstHostReceivers = dst.hosts.removeAll(src.hosts)
                val dstHostSenders = dst.hosts.removeAll(dstHostReceivers)

                // TODO: optimize this later. receivers don't necessarily need to receive from all senders
                ProtocolCommunication(mapOf(
                    "send" to
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
                        ).toSet(),

                    "broadcast" to setOf()
                ))
            }

            // TODO: fix this later
            src is ABY && dst is ABY -> {
                ProtocolCommunication(mapOf(
                    "send" to
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
                        },

                    "broadcast" to setOf()
                ))
            }

            else -> throw Error("does not support communication from ${src.protocolName} to ${dst.protocolName}")
        }
    }

    override fun communicate(src: Protocol, dst: Protocol): ProtocolCommunication =
        Pair(src, dst).communicate

    fun getSendPhase(src: Protocol, dst: Protocol): ProtocolCommunication.CommunicationPhase =
        Pair(src, dst).communicate.getPhase("send")

    fun getBroadcastPhase(src: Protocol, dst: Protocol): ProtocolCommunication.CommunicationPhase =
        Pair(src, dst).communicate.getPhase("broadcast")

    private val Pair<Protocol, Protocol>.synchronize: ProtocolCommunication by attribute {
        val src = this.first
        val dst = this.second
        val dstHostReceivers = dst.hosts.removeAll(src.hosts)
        val dstHostSenders = dst.hosts.removeAll(dstHostReceivers)
        ProtocolCommunication(mapOf(
            "sync" to
                dstHostSenders.map { sender ->
                    CommunicationEvent(src.syncOutputPorts[sender]!!, dst.syncInputPorts[sender]!!)
                }.plus(
                    src.hosts.flatMap { sender ->
                        dstHostReceivers.map { receiver ->
                            CommunicationEvent(src.syncOutputPorts[sender]!!, dst.syncInputPorts[receiver]!!)
                        }
                    }
                ).toSet()
        ))
    }

    fun getSyncPhase(src: Protocol, dst: Protocol): ProtocolCommunication.CommunicationPhase =
        Pair(src, dst).synchronize.getPhase("sync")
}
