package io.github.apl_cornell.viaduct.syntax.surface

import io.github.apl_cornell.viaduct.prettyprinting.AnsiBaseColor
import io.github.apl_cornell.viaduct.prettyprinting.AnsiColor
import io.github.apl_cornell.viaduct.prettyprinting.Document
import io.github.apl_cornell.viaduct.prettyprinting.NormalColor
import io.github.apl_cornell.viaduct.prettyprinting.Style
import io.github.apl_cornell.viaduct.prettyprinting.styled

/** The display style used for the keyword in the language. */
object KeywordStyle : Style {
    override val foregroundColor: AnsiColor
        get() = NormalColor(AnsiBaseColor.GREEN)
}

/** Converts [keyword] into a [Document] and applies [KeywordStyle] to it. */
internal fun keyword(keyword: String): Document =
    Document(keyword).styled(KeywordStyle)
