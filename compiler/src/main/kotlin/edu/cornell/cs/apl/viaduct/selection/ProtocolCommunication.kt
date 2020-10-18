package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.InputPort
import edu.cornell.cs.apl.viaduct.syntax.OutputPort

data class CommunicationEvent(val send: OutputPort, val recv: InputPort)

data class ProtocolCommunication(
    val events: Set<CommunicationEvent>
) : Set<CommunicationEvent> by events {
    fun getHostSends(h: Host): Set<CommunicationEvent> =
        events.filter { event -> event.send.host == h }.toSet()

    fun getHostReceives(h: Host): Set<CommunicationEvent> =
        events.filter { event -> event.recv.host == h }.toSet()

    fun getHostCommunication(h: Host): Set<CommunicationEvent> =
        events.filter { event -> event.recv.host == h || event.send.host == h }.toSet()
}
