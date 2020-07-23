package edu.cornell.cs.apl.viaduct.syntax

/**
 * Build protocol objects from list of participants.
 *
 * This is used by the elaborator to parse GenericProtocols into actual protocol objects.
 */
interface ProtocolFactory {
    val protocolName: String

    fun buildProtocol(participants: List<Host>): Protocol
}
