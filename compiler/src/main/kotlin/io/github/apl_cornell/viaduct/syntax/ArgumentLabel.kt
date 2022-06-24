package io.github.apl_cornell.viaduct.syntax

import io.github.apl_cornell.apl.prettyprinting.Document
import io.github.apl_cornell.apl.prettyprinting.Style
import io.github.apl_cornell.apl.prettyprinting.styled

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
