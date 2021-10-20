package edu.cornell.cs.apl.viaduct.backends.commitment

import edu.cornell.cs.apl.viaduct.backends.cleartext.Local
import edu.cornell.cs.apl.viaduct.backends.cleartext.Replication
import edu.cornell.cs.apl.viaduct.selection.AbstractProtocolComposer
import edu.cornell.cs.apl.viaduct.selection.CommunicationEvent
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode

object CommitmentProtocolComposer : AbstractProtocolComposer() {
    override fun communicationEvents(source: Protocol, destination: Protocol): Iterable<CommunicationEvent>? =
        when {
            source is Local && destination is Commitment && source.host == destination.cleartextHost -> {
                setOf(CommunicationEvent(source.outputPort, destination.inputPort))
            }

            source is Replication && destination is Commitment && source.hosts.containsAll(destination.hosts) -> {
                destination.cleartextInputPorts.map { port ->
                    CommunicationEvent(source.hostOutputPorts.getValue(port.key), port.value)
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

            else -> super.communicationEvents(source, destination)
        }

    override fun mandatoryParticipatingHosts(protocol: Protocol, statement: LetNode): Set<Host> =
        protocol.hosts

    override fun visibleGuardHosts(protocol: Protocol): Set<Host> =
        setOf((protocol as Commitment).cleartextHost)
}
