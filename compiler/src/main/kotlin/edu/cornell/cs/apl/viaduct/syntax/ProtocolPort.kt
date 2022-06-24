package edu.cornell.cs.apl.viaduct.syntax

typealias PortId = String

interface ProtocolPort {
    val protocol: Protocol
    val host: Host
    val id: PortId

    fun asProjection(): ProtocolProjection = ProtocolProjection(protocol, host)
}

data class InputPort(
    override val protocol: Protocol,
    override val host: Host,
    override val id: PortId
) : ProtocolPort

data class OutputPort(
    override val protocol: Protocol,
    override val host: Host,
    override val id: PortId
) : ProtocolPort
