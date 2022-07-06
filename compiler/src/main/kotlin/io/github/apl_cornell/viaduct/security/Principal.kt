package io.github.apl_cornell.viaduct.security

import io.github.apl_cornell.viaduct.prettyprinting.Document
import io.github.apl_cornell.viaduct.prettyprinting.PrettyPrintable
import io.github.apl_cornell.viaduct.prettyprinting.Style
import io.github.apl_cornell.viaduct.prettyprinting.styled

/** An actor with an associated security label. */
data class Principal(val name: String) : Comparable<Principal>, PrettyPrintable {
    override fun compareTo(other: Principal): Int =
        name.compareTo(other.name)

    override fun toDocument(): Document = Document(name).styled(PrincipalStyle)

    // TODO: remove and use [toDocument]
    override fun toString(): String =
        name
}

/** The display style of [Principal]s. */
object PrincipalStyle : Style
