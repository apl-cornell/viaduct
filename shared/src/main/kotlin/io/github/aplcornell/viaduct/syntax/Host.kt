package io.github.aplcornell.viaduct.syntax

import io.github.aplcornell.viaduct.prettyprinting.Document
import io.github.aplcornell.viaduct.prettyprinting.Style
import io.github.aplcornell.viaduct.prettyprinting.styled

/**
 * A participant in the distributed computation.
 *
 * A host is a location that can run (one or more) processes;
 * it has inputs and outputs.
 */
data class Host(override val name: String) : Name, Comparable<Host> {
    override val nameCategory: String
        get() = "host"

    override fun compareTo(other: Host): Int = this.name.compareTo(other.name)

    override fun toDocument(): Document = Document(name).styled(HostStyle)
}

/** The display style of [Host]s. */
object HostStyle : Style
