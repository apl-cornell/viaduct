package io.github.aplcornell.viaduct.selection

import io.github.aplcornell.viaduct.syntax.Host
import io.github.aplcornell.viaduct.syntax.InputPort
import io.github.aplcornell.viaduct.syntax.OutputPort
import io.github.aplcornell.viaduct.syntax.PortId
import io.github.aplcornell.viaduct.syntax.ProtocolProjection

data class CommunicationEvent(val send: OutputPort, val recv: InputPort)

data class ProtocolCommunication(
    val events: Set<CommunicationEvent>
) : Set<CommunicationEvent> by events {
    fun getHostSends(h: Host, portId: PortId? = null): Set<CommunicationEvent> =
        events.filter { event ->
            event.send.host == h && (portId?.let { event.send.id == it } ?: true)
        }.toSet()

    fun getProjectionSends(projection: ProtocolProjection, portId: PortId? = null): Set<CommunicationEvent> =
        events.filter { event ->
            event.send.host == projection.host &&
                event.send.protocol == projection.protocol &&
                (portId?.let { event.send.id == it } ?: true)
        }.toSet()

    fun getHostReceives(h: Host, portId: PortId? = null): Set<CommunicationEvent> =
        events.filter { event ->
            event.recv.host == h && (portId?.let { event.recv.id == it } ?: true)
        }.toSet()

    fun getProjectionReceives(projection: ProtocolProjection, portId: PortId? = null): Set<CommunicationEvent> =
        events.filter { event ->
            event.recv.host == projection.host &&
                event.recv.protocol == projection.protocol &&
                (portId?.let { event.recv.id == it } ?: true)
        }.toSet()
}
