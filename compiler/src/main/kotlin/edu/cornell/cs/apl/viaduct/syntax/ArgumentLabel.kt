package edu.cornell.cs.apl.viaduct.syntax

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.Style
import edu.cornell.cs.apl.prettyprinting.styled

/** The name assigned to an argument. */
data class ArgumentLabel(override val name: String) : Name, Comparable<ArgumentLabel> {
    override val nameCategory: String
        get() = "argument"

    override fun compareTo(other: ArgumentLabel): Int =
        this.name.compareTo(other.name)

    override fun asDocument(): Document = Document(name).styled(ArgumentLabelStyle)
}

/** The display style of [ArgumentLabel]s. */
object ArgumentLabelStyle : Style
