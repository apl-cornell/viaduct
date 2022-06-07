package io.github.apl_cornell.viaduct.syntax

import io.github.apl_cornell.apl.prettyprinting.AnsiBaseColor
import io.github.apl_cornell.apl.prettyprinting.AnsiColor
import io.github.apl_cornell.apl.prettyprinting.Document
import io.github.apl_cornell.apl.prettyprinting.NormalColor
import io.github.apl_cornell.apl.prettyprinting.Style
import io.github.apl_cornell.apl.prettyprinting.styled

/** The target label for unstructured control statements like `continue` and `break`. */
data class JumpLabel(override val name: String) : Name {
    override val nameCategory: String
        get() = "label"

    override fun toDocument(): Document = Document(name).styled(JumpLabelStyle)
}

/** The display style of [JumpLabel]s. */
object JumpLabelStyle : Style {
    override val foregroundColor: AnsiColor
        get() = NormalColor(AnsiBaseColor.BLUE)
}
