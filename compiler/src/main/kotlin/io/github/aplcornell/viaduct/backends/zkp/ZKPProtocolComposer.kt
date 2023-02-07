package io.github.aplcornell.viaduct.backends.zkp

import io.github.aplcornell.viaduct.backends.cleartext.Local
import io.github.aplcornell.viaduct.backends.cleartext.Replication
import io.github.aplcornell.viaduct.selection.AbstractProtocolComposer
import io.github.aplcornell.viaduct.selection.CommunicationEvent
import io.github.aplcornell.viaduct.syntax.Host
import io.github.aplcornell.viaduct.syntax.Protocol
import io.github.aplcornell.viaduct.syntax.intermediate.LetNode

object ZKPProtocolComposer : AbstractProtocolComposer() {
    override fun communicationEvents(source: Protocol, destination: Protocol): Iterable<CommunicationEvent>? =
        when {
            source is Local && destination is ZKP && source.host == destination.prover -> {
                setOf(CommunicationEvent(source.outputPort, destination.secretInputPort))
            }

            source is Replication && destination is ZKP && source.hosts.containsAll(destination.hosts) -> {
                destination.cleartextInput.map { port ->
                    CommunicationEvent(source.hostOutputPorts.getValue(port.key), port.value)
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
        protocol.hosts

    override fun visibleGuardHosts(protocol: Protocol): Set<Host> =
        setOf((protocol as ZKP).prover)
}
