package io.github.aplcornell.viaduct.syntax

import io.github.aplcornell.viaduct.prettyprinting.Document
import io.github.aplcornell.viaduct.prettyprinting.Style
import io.github.aplcornell.viaduct.prettyprinting.styled

/**
 * The name of a cryptographic protocol.
 *
 * A [Protocol] is (essentially) a [ProtocolName] applied to arguments.
 */
data class ProtocolName(override val name: String) : Name, Comparable<ProtocolName> {
    override val nameCategory: String
        get() = "protocol"

    override fun compareTo(other: ProtocolName): Int = this.name.compareTo(other.name)

    override fun toDocument(): Document = Document(name).styled(ProtocolNameStyle)
}

/** The display style of [ProtocolName]s. */
object ProtocolNameStyle : Style
