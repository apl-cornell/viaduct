package io.github.apl_cornell.viaduct.security

import io.github.apl_cornell.viaduct.prettyprinting.Document
import io.github.apl_cornell.viaduct.prettyprinting.PrettyPrintable
import io.github.apl_cornell.viaduct.syntax.Host
import io.github.apl_cornell.viaduct.syntax.LabelVariable

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
