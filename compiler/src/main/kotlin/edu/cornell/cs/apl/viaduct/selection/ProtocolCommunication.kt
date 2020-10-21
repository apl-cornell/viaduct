package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.InputPort
import edu.cornell.cs.apl.viaduct.syntax.OutputPort
import edu.cornell.cs.apl.viaduct.syntax.PortId

data class CommunicationEvent(val send: OutputPort, val recv: InputPort)

data class ProtocolCommunication(
    val events: Set<CommunicationEvent>
) : Set<CommunicationEvent> by events {
    fun getHostSends(h: Host, portId: PortId? = null): Set<CommunicationEvent> =
        events.filter { event ->
            event.send.host == h && (portId?.let { event.send.id == it } ?: true)
        }.toSet()

    fun getHostReceives(h: Host, portId: PortId? = null): Set<CommunicationEvent> =
        events.filter { event ->
            event.recv.host == h && (portId?.let { event.recv.id == it } ?: true)
        }.toSet()

    fun getHostCommunication(h: Host): Set<CommunicationEvent> =
        events.filter { event -> event.recv.host == h || event.send.host == h }.toSet()
}
