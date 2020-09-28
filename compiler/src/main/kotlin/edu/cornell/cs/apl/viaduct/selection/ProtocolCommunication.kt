package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.InputPort
import edu.cornell.cs.apl.viaduct.syntax.OutputPort

data class CommunicationEvent(val send: OutputPort, val recv: InputPort)

typealias CommunicationPhaseId = String

class ProtocolCommunication(initMap: Map<CommunicationPhaseId, Set<CommunicationEvent>>) {
    val communicationMap: Map<CommunicationPhaseId, CommunicationPhase> =
        initMap.map { kv -> Pair(kv.key, CommunicationPhase(kv.value)) }.toMap()

    inner class CommunicationPhase(
        val events: Set<CommunicationEvent>
    ) : Set<CommunicationEvent> by events {
        fun getHostSends(h: Host): Set<CommunicationEvent> =
            events.filter { event -> event.send.host == h }.toSet()

        fun getHostReceives(h: Host): Set<CommunicationEvent> =
            events.filter { event -> event.recv.host == h }.toSet()

        fun getHostCommunication(h: Host): Set<CommunicationEvent> =
            events.filter { event -> event.recv.host == h || event.send.host == h }.toSet()
    }

    fun getPhase(phase: CommunicationPhaseId): CommunicationPhase = communicationMap[phase]!!
}
