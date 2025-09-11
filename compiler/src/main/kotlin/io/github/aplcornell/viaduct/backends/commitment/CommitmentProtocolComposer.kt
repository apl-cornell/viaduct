package io.github.aplcornell.viaduct.backends.commitment

import io.github.aplcornell.viaduct.backends.cleartext.Local
import io.github.aplcornell.viaduct.backends.cleartext.Replication
import io.github.aplcornell.viaduct.selection.AbstractProtocolComposer
import io.github.aplcornell.viaduct.selection.CommunicationEvent
import io.github.aplcornell.viaduct.syntax.Host
import io.github.aplcornell.viaduct.syntax.Protocol
import io.github.aplcornell.viaduct.syntax.intermediate.LetNode

object CommitmentProtocolComposer : AbstractProtocolComposer() {
    override fun communicationEvents(
        source: Protocol,
        destination: Protocol,
    ): Iterable<CommunicationEvent>? =
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
                    CommunicationEvent(source.openCleartextOutputPort, destination.cleartextCommitmentInputPort),
                ).plus(
                    source.openCommitmentOutputPorts.values.map {
                        CommunicationEvent(it, destination.hashCommitmentInputPort)
                    },
                )
            }

            source is Commitment && destination is Replication -> {
                destination.hosts.flatMap { host ->
                    listOf(
                        CommunicationEvent(
                            source.openCleartextOutputPort,
                            destination.hostCleartextCommitmentInputPorts.getValue(host),
                        ),
                    ).plus(
                        source.openCommitmentOutputPorts.values.map {
                            CommunicationEvent(it, destination.hostHashCommitmentInputPorts.getValue(host))
                        },
                    )
                }
            }

            else -> super.communicationEvents(source, destination)
        }

    override fun mandatoryParticipatingHosts(
        protocol: Protocol,
        statement: LetNode,
    ): Set<Host> = protocol.hosts

    override fun visibleGuardHosts(protocol: Protocol): Set<Host> = setOf((protocol as Commitment).cleartextHost)
}
