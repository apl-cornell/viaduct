package edu.cornell.cs.apl.viaduct.syntax

import edu.cornell.cs.apl.prettyprinting.AnsiBaseColor
import edu.cornell.cs.apl.prettyprinting.AnsiColor
import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.NormalColor
import edu.cornell.cs.apl.prettyprinting.Style
import edu.cornell.cs.apl.prettyprinting.styled

/** The target label for unstructured control statements like `continue` and `break`. */
data class JumpLabel(override val name: String) : Name {
    override val nameCategory: String
        get() = "label"

    override val asDocument: Document
        get() = Document(name).styled(JumpLabelStyle)
}

/** The display style of [JumpLabel]s. */
object JumpLabelStyle : Style {
    override val foregroundColor: AnsiColor
        get() = NormalColor(AnsiBaseColor.BLUE)
}
