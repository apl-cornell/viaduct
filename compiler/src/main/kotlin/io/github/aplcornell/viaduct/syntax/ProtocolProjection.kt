package io.github.aplcornell.viaduct.syntax

import io.github.aplcornell.viaduct.prettyprinting.Document

data class ProtocolProjection(
    val protocol: Protocol,
    val host: Host,
) : Name, Comparable<ProtocolProjection> {
    override val name = toString()

    override val nameCategory = "ProtocolProjection"

    override fun toDocument(): Document = Document(name)

    override fun toString(): String {
        return "${protocol.name}@$host"
    }

    override fun compareTo(other: ProtocolProjection): Int {
        return compareBy(ProtocolProjection::protocol, ProtocolProjection::host).compare(this, other)
    }
}
