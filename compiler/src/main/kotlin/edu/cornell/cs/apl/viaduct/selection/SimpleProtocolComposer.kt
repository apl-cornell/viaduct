package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.viaduct.backends.aby.ABY
import edu.cornell.cs.apl.viaduct.backends.aby.ArithABY
import edu.cornell.cs.apl.viaduct.backends.aby.BoolABY
import edu.cornell.cs.apl.viaduct.backends.aby.YaoABY
import edu.cornell.cs.apl.viaduct.backends.cleartext.Local
import edu.cornell.cs.apl.viaduct.backends.cleartext.Replication
import edu.cornell.cs.apl.viaduct.backends.commitment.Commitment
import edu.cornell.cs.apl.viaduct.backends.zkp.ZKP
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.InputPort
import edu.cornell.cs.apl.viaduct.syntax.OutputPort
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode

val SimpleProtocolComposer: ProtocolComposer = UncachedSimpleProtocolComposer.cached()

/** Describe how protocols should communicate / compose with each other. */
private object UncachedSimpleProtocolComposer : AbstractProtocolComposer() {
    override fun communicationEvents(source: Protocol, destination: Protocol): Iterable<CommunicationEvent>? =
        when {
            source is Local && destination is Local && source.host != destination.host -> {
                setOf(CommunicationEvent(source.outputPort, destination.inputPort))
            }

            source is Local && destination is Replication -> {
                destination.hostInputPorts.values.map { CommunicationEvent(source.outputPort, it) }
            }

            source is Local && destination is ABY && destination.hosts.contains(source.host) -> {
                setOf(CommunicationEvent(source.outputPort, destination.secretInputPorts.getValue(source.host)))
            }

            source is Local && destination is Commitment && source.host == destination.cleartextHost -> {
                setOf(CommunicationEvent(source.outputPort, destination.inputPort))
            }

            source is Local && destination is ZKP && source.host == destination.prover -> {
                setOf(CommunicationEvent(source.outputPort, destination.secretInputPort))
            }

            source is Replication && destination is Local -> {
                if (source.hosts.contains(destination.host)) {
                    setOf(CommunicationEvent(source.hostOutputPorts.getValue(destination.host), destination.inputPort))
                } else {
                    source.hostOutputPorts.values.map { CommunicationEvent(it, destination.inputPort) }
                }
            }

            source is Replication && destination is Replication -> {
                val dstHostReceivers = destination.hosts.removeAll(source.hosts)
                val dstHostSenders = destination.hosts.removeAll(dstHostReceivers)

                // TODO: optimize this later. receivers don't necessarily need to receive from all senders
                dstHostSenders.map { sender ->
                    CommunicationEvent(source.hostOutputPorts.getValue(sender), destination.hostInputPorts.getValue(sender))
                }.plus(
                    source.hosts.flatMap { sender ->
                        dstHostReceivers.map { receiver ->
                            CommunicationEvent(source.hostOutputPorts.getValue(sender), destination.hostInputPorts.getValue(receiver))
                        }
                    }
                )
            }

            source is Replication && destination is ABY && source.hosts.containsAll(destination.hosts) -> {
                destination.cleartextInputPorts.map { port ->
                    CommunicationEvent(source.hostOutputPorts.getValue(port.key), port.value)
                }
            }

            source is Replication && destination is Commitment && source.hosts.containsAll(destination.hosts) -> {
                destination.cleartextInputPorts.map { port ->
                    CommunicationEvent(source.hostOutputPorts.getValue(port.key), port.value)
                }
            }

            source is Replication && destination is ZKP && source.hosts.containsAll(destination.hosts) -> {
                destination.cleartextInput.map { port ->
                    CommunicationEvent(source.hostOutputPorts.getValue(port.key), port.value)
                }
            }

            source is ABY && destination is Local && source.hosts.contains(destination.host) -> {
                setOf(CommunicationEvent(source.cleartextOutputPorts.getValue(destination.host), destination.inputPort))
            }

            source is ABY && destination is Replication && source.hosts.containsAll(destination.hosts) -> {
                destination.hostInputPorts.map { port ->
                    CommunicationEvent(source.cleartextOutputPorts.getValue(port.key), port.value)
                }
            }

            source is ABY && destination is ABY && source.client == destination.client && source.server == destination.server -> {
                val outputPorts: Map<Host, OutputPort>
                val inputPorts: Map<Host, InputPort>

                when {
                    source is ArithABY && destination is BoolABY -> {
                        outputPorts = source.A2BOutputPorts
                        inputPorts = destination.A2BInputPorts
                    }

                    source is ArithABY && destination is YaoABY -> {
                        outputPorts = source.A2YOutputPorts
                        inputPorts = destination.A2YInputPorts
                    }

                    source is BoolABY && destination is ArithABY -> {
                        outputPorts = source.B2AOutputPorts
                        inputPorts = destination.B2AInputPorts
                    }

                    source is BoolABY && destination is YaoABY -> {
                        outputPorts = source.B2YOutputPorts
                        inputPorts = destination.B2YInputPorts
                    }

                    source is YaoABY && destination is ArithABY -> {
                        outputPorts = source.Y2AOutputPorts
                        inputPorts = destination.Y2AInputPorts
                    }

                    source is YaoABY && destination is BoolABY -> {
                        outputPorts = source.Y2BOutputPorts
                        inputPorts = destination.Y2BInputPorts
                    }

                    else -> {
                        outputPorts = source.internalOutputPorts
                        inputPorts = destination.internalInputPorts
                    }
                }

                outputPorts.map { outputPort ->
                    CommunicationEvent(outputPort.value, inputPorts.getValue(outputPort.key))
                }
            }

            source is Commitment && destination is Local -> {
                setOf(
                    CommunicationEvent(source.openCleartextOutputPort, destination.cleartextCommitmentInputPort)
                ).plus(
                    source.openCommitmentOutputPorts.values.map {
                        CommunicationEvent(it, destination.hashCommitmentInputPort)
                    }
                )
            }

            source is Commitment && destination is Replication -> {
                destination.hosts.flatMap { host ->
                    listOf(
                        CommunicationEvent(
                            source.openCleartextOutputPort,
                            destination.hostCleartextCommitmentInputPorts.getValue(host)
                        )
                    ).plus(
                        source.openCommitmentOutputPorts.values.map {
                            CommunicationEvent(it, destination.hostHashCommitmentInputPorts.getValue(host))
                        }
                    )
                }
            }

            source is ZKP && destination is Local && source.hosts.contains(destination.host) -> {
                setOf(CommunicationEvent(source.outputPorts.getValue(destination.host), destination.inputPort))
            }

            source is ZKP && destination is Replication && source.hosts.containsAll(destination.hosts) -> {
                destination.hostInputPorts.map { port ->
                    CommunicationEvent(source.outputPorts.getValue(port.key), port.value)
                }
            }

            else -> super.communicationEvents(source, destination)
        }

    override fun mandatoryParticipatingHosts(protocol: Protocol, statement: LetNode): Set<Host> =
        when (protocol) {
            is ABY, is Local, is Commitment, is ZKP -> protocol.hosts
            is Replication -> setOf()
            else -> setOf()
        }

    override fun visibleGuardHosts(protocol: Protocol): Set<Host> =
        when (protocol) {
            is Local, is Replication -> protocol.hosts
            is ABY -> setOf()
            is Commitment -> setOf(protocol.cleartextHost)
            else -> setOf()
        }
}
