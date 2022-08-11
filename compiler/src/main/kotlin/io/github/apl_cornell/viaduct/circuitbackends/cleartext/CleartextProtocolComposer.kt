package io.github.apl_cornell.viaduct.circuitbackends.cleartext

// import io.github.apl_cornell.viaduct.selection.AbstractProtocolComposer
// import io.github.apl_cornell.viaduct.selection.CommunicationEvent
// import io.github.apl_cornell.viaduct.syntax.Host
// import io.github.apl_cornell.viaduct.syntax.Protocol
//
// object CleartextProtocolComposer : AbstractProtocolComposer() {
//    override fun communicationEvents(source: Protocol, destination: Protocol): Iterable<CommunicationEvent>? =
//        when {
//            source is Local && destination is Local && source.host != destination.host -> {
//                setOf(CommunicationEvent(source.outputPort, destination.inputPort))
//            }
//
//            source is Local && destination is Replication -> {
//                destination.hostInputPorts.values.map { CommunicationEvent(source.outputPort, it) }
//            }
//
//            source is Replication && destination is Local -> {
//                if (source.hosts.contains(destination.host)) {
//                    setOf(CommunicationEvent(source.hostOutputPorts.getValue(destination.host), destination.inputPort))
//                } else {
//                    source.hostOutputPorts.values.map { CommunicationEvent(it, destination.inputPort) }
//                }
//            }
//
//            source is Replication && destination is Replication -> {
//                val dstHostReceivers = destination.hosts.removeAll(source.hosts)
//                val dstHostSenders = destination.hosts.removeAll(dstHostReceivers)
//
//                // TODO: optimize this later. receivers don't necessarily need to receive from all senders
//                dstHostSenders.map { sender ->
//                    CommunicationEvent(
//                        source.hostOutputPorts.getValue(sender),
//                        destination.hostInputPorts.getValue(sender)
//                    )
//                }.plus(
//                    source.hosts.flatMap { sender ->
//                        dstHostReceivers.map { receiver ->
//                            CommunicationEvent(
//                                source.hostOutputPorts.getValue(sender),
//                                destination.hostInputPorts.getValue(receiver)
//                            )
//                        }
//                    }
//                )
//            }
//
//            else -> super.communicationEvents(source, destination)
//        }
//
// //    override fun mandatoryParticipatingHosts(protocol: Protocol, statement: LetNode): Set<Host> =
// //        protocol.hosts
//
//    override fun visibleGuardHosts(protocol: Protocol): Set<Host> =
//        protocol.hosts
// }
