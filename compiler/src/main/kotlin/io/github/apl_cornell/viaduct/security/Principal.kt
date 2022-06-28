package io.github.apl_cornell.viaduct.security

import io.github.apl_cornell.apl.prettyprinting.Document
import io.github.apl_cornell.apl.prettyprinting.PrettyPrintable
import io.github.apl_cornell.viaduct.syntax.Host
import io.github.apl_cornell.viaduct.syntax.LabelVariable

/** An actor with an associated security label. */
sealed class Principal : PrettyPrintable

data class HostPrincipal(val host: Host) : Principal() {
    override fun toDocument(): Document = host.toDocument()
}

data class PolymorphicPrincipal(val labelVariable: LabelVariable) : Principal() {
    override fun toDocument(): Document = labelVariable.toDocument()
}
