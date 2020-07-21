package edu.cornell.cs.apl.viaduct.syntax.surface

import edu.cornell.cs.apl.prettyprinting.AnsiBaseColor
import edu.cornell.cs.apl.prettyprinting.AnsiColor
import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.NormalColor
import edu.cornell.cs.apl.prettyprinting.Style
import edu.cornell.cs.apl.prettyprinting.styled

/** The display style used for the keyword in the language. */
object KeywordStyle : Style {
    override val foregroundColor: AnsiColor
        get() = NormalColor(AnsiBaseColor.GREEN)
}

/** Converts [keyword] into a [Document] and applies [KeywordStyle] to it. */
internal fun keyword(keyword: String): Document =
    Document(keyword).styled(KeywordStyle)
