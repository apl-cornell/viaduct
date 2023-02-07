package io.github.aplcornell.viaduct.security

import io.github.aplcornell.viaduct.prettyprinting.Document
import io.github.aplcornell.viaduct.prettyprinting.PrettyPrintable
import io.github.aplcornell.viaduct.syntax.Host
import io.github.aplcornell.viaduct.syntax.LabelVariable

/** An actor with an associated security label. */
sealed class Principal : PrettyPrintable

data class HostPrincipal(val host: Host) : Principal() {
    override fun toString(): String = "${host.name}"
    override fun toDocument(): Document = host.toDocument()
}

data class PolymorphicPrincipal(val labelVariable: LabelVariable) : Principal() {
    override fun toString(): String = "${labelVariable.name}"
    override fun toDocument(): Document = labelVariable.toDocument()
}
