package edu.cornell.cs.apl.viaduct.syntax.surface

import edu.cornell.cs.apl.prettyprinting.AnsiBaseColor
import edu.cornell.cs.apl.prettyprinting.AnsiColor
import edu.cornell.cs.apl.prettyprinting.NormalColor
import edu.cornell.cs.apl.prettyprinting.Style

/** The display style used for the keyword in the language. */
object KeywordStyle : Style {
    override val foregroundColor: AnsiColor
        get() = NormalColor(AnsiBaseColor.GREEN)
}
