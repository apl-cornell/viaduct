package edu.cornell.cs.apl.viaduct.backends.aby

import edu.cornell.cs.apl.viaduct.backends.cleartext.Local
import edu.cornell.cs.apl.viaduct.backends.cleartext.Replication
import edu.cornell.cs.apl.viaduct.selection.AbstractProtocolComposer
import edu.cornell.cs.apl.viaduct.selection.CommunicationEvent
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.InputPort
import edu.cornell.cs.apl.viaduct.syntax.OutputPort
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode

object ABYProtocolComposer : AbstractProtocolComposer() {
    override fun communicationEvents(source: Protocol, destination: Protocol): Iterable<CommunicationEvent>? =
        when {
            source is Local && destination is ABY && destination.hosts.contains(source.host) -> {
                setOf(CommunicationEvent(source.outputPort, destination.secretInputPorts.getValue(source.host)))
            }

            source is Replication && destination is ABY && source.hosts.containsAll(destination.hosts) -> {
                destination.cleartextInputPorts.map { port ->
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

            else -> super.communicationEvents(source, destination)
        }

    override fun mandatoryParticipatingHosts(protocol: Protocol, statement: LetNode): Set<Host> =
        protocol.hosts

    override fun visibleGuardHosts(protocol: Protocol): Set<Host> =
        setOf()
}
