package edu.cornell.cs.apl.viaduct.syntax

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.Style
import edu.cornell.cs.apl.prettyprinting.styled

/**
 * A participant in the distributed computation.
 *
 * A host is a location that can run (one or more) processes;
 * it has inputs and outputs.
 */
data class Host(override val name: String) : Name, Comparable<Host> {
    override val nameCategory: String
        get() = "host"

    override fun compareTo(other: Host): Int =
        this.name.compareTo(other.name)

    override val asDocument: Document
        get() = Document(name).styled(HostStyle)
}

/** The display style of [Host]s. */
object HostStyle : Style
