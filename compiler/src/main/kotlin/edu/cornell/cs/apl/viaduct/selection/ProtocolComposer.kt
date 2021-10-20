package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.attributes.attribute
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.ProtocolName
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SimpleStatementNode

abstract class ProtocolComposer {
    fun communicate(source: Protocol, destination: Protocol): ProtocolCommunication =
        communicateOrNull(source, destination)
            ?: throw IllegalArgumentException("Cannot communicate from $source to $destination.")

    fun canCommunicate(source: Protocol, destination: Protocol): Boolean =
        communicateOrNull(source, destination) != null

    abstract fun communicateOrNull(source: Protocol, destination: Protocol): ProtocolCommunication?

    abstract fun mandatoryParticipatingHosts(protocol: Protocol, statement: SimpleStatementNode): Set<Host>

    abstract fun visibleGuardHosts(protocol: Protocol): Set<Host>
}

/** A [ProtocolComposer] with sensible defaults. */
abstract class AbstractProtocolComposer : ProtocolComposer() {
    /**
     * Returns mandatory participating hosts for [LetNode]s only from which
     * mandatory participants for all other statements are derived.
     */
    protected abstract fun mandatoryParticipatingHosts(protocol: Protocol, statement: LetNode): Set<Host>

    final override fun mandatoryParticipatingHosts(protocol: Protocol, statement: SimpleStatementNode): Set<Host> =
        when (statement) {
            is LetNode -> mandatoryParticipatingHosts(protocol, statement)

            else -> protocol.hosts
        }
}

/** Combines multiple protocol composers into one. */
fun Iterable<Pair<Set<ProtocolName>, ProtocolComposer>>.unions(): ProtocolComposer =
    object : ProtocolComposer() {
        private val protocolComposers = this@unions.flatMap { it.first.map { name -> name to it.second } }.toMap()

        private fun composerFor(protocol: Protocol): ProtocolComposer =
            // TODO: more specific error message if no composer exists for protocol.
            protocolComposers.getValue(protocol.protocolName)

        override fun communicateOrNull(source: Protocol, destination: Protocol): ProtocolCommunication? =
            // TODO: this should check for duplicates
            protocolComposers.values.firstNotNullOfOrNull { it.communicateOrNull(source, destination) }

        override fun mandatoryParticipatingHosts(protocol: Protocol, statement: SimpleStatementNode): Set<Host> =
            composerFor(protocol).mandatoryParticipatingHosts(protocol, statement)

        override fun visibleGuardHosts(protocol: Protocol): Set<Host> =
            composerFor(protocol).visibleGuardHosts(protocol)
    }

/** Caches values returned by [ProtocolComposer.communicateOrNull] so it is called once per source/destination pair. */
fun ProtocolComposer.cached(): ProtocolComposer =
    object : ProtocolComposer() {
        /** Cache [communicateOrNull]. */
        private val Pair<Protocol, Protocol>.communicateOrNull: ProtocolCommunication? by attribute {
            this@cached.communicateOrNull(first, second)
        }

        override fun communicateOrNull(source: Protocol, destination: Protocol): ProtocolCommunication? =
            (source to destination).communicateOrNull

        override fun mandatoryParticipatingHosts(protocol: Protocol, statement: SimpleStatementNode): Set<Host> =
            this@cached.mandatoryParticipatingHosts(protocol, statement)

        override fun visibleGuardHosts(protocol: Protocol): Set<Host> =
            this@cached.visibleGuardHosts(protocol)
    }
