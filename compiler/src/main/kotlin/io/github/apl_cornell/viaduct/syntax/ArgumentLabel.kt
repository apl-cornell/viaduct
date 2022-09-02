package io.github.apl_cornell.viaduct.syntax

import io.github.apl_cornell.viaduct.prettyprinting.Document
import io.github.apl_cornell.viaduct.prettyprinting.Style
import io.github.apl_cornell.viaduct.prettyprinting.styled

/** The name assigned to an argument. */
data class ArgumentLabel(override val name: String) : Name, Comparable<ArgumentLabel> {
    override val nameCategory: String
        get() = "argument"

    override fun compareTo(other: ArgumentLabel): Int =
        this.name.compareTo(other.name)

    override fun toDocument(): Document = Document(name).styled(ArgumentLabelStyle)
}

/** The display style of [ArgumentLabel]s. */
object ArgumentLabelStyle : Style
