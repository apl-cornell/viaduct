package io.github.aplcornell.viaduct.syntax

import io.github.aplcornell.viaduct.prettyprinting.AnsiBaseColor
import io.github.aplcornell.viaduct.prettyprinting.AnsiColor
import io.github.aplcornell.viaduct.prettyprinting.Document
import io.github.aplcornell.viaduct.prettyprinting.NormalColor
import io.github.aplcornell.viaduct.prettyprinting.Style
import io.github.aplcornell.viaduct.prettyprinting.styled

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
