package edu.cornell.cs.apl.viaduct.syntax

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.Style
import edu.cornell.cs.apl.prettyprinting.styled

/**
 * The name of a cryptographic protocol.
 *
 * A [Protocol] is (essentially) a [ProtocolName] applied to arguments.
 */
data class ProtocolName(override val name: String) : Name, Comparable<ProtocolName> {
    override val nameCategory: String
        get() = "protocol"

    override fun compareTo(other: ProtocolName): Int =
        this.name.compareTo(other.name)

    override val asDocument: Document
        get() = Document(name).styled(ProtocolNameStyle)
}

/** The display style of [ProtocolName]s. */
object ProtocolNameStyle : Style
