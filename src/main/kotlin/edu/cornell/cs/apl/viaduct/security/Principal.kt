package edu.cornell.cs.apl.viaduct.security

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.PrettyPrintable
import edu.cornell.cs.apl.prettyprinting.Style
import edu.cornell.cs.apl.prettyprinting.styled

/** An actor with an associated security label. */
data class Principal(val name: String) : Comparable<Principal>, PrettyPrintable {
    override fun compareTo(other: Principal): Int =
        name.compareTo(other.name)

    override val asDocument: Document
        get() = Document(name).styled(PrincipalStyle)

    // TODO: remove and use [asDocument]
    override fun toString(): String =
        name
}

/** The display style of [Principal]s. */
object PrincipalStyle : Style
