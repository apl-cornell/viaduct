package io.github.apl_cornell.viaduct.security

import io.github.apl_cornell.apl.prettyprinting.Document
import io.github.apl_cornell.apl.prettyprinting.PrettyPrintable
import io.github.apl_cornell.apl.prettyprinting.Style
import io.github.apl_cornell.apl.prettyprinting.styled
import io.github.apl_cornell.viaduct.syntax.Host
import io.github.apl_cornell.viaduct.syntax.LabelVariable

/** An actor with an associated security label. */
sealed class Principal(val name: String) : Comparable<Principal>,
    PrettyPrintable {
    override fun compareTo(other: Principal): Int =
        name.compareTo(other.name)

    override fun toDocument(): Document = Document(name).styled(PrincipalStyle)

    // TODO: remove and use [toDocument]
    override fun toString(): String =
        name
}

class HostPrincipal(host: Host) : Principal(host.name)

class PolymorphicPrincipal(labelVariable: LabelVariable) : Principal(labelVariable.name)


/** The display style of [Principal]s. */
object PrincipalStyle : Style
