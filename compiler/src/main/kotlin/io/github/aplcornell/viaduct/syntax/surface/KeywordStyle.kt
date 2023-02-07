package io.github.aplcornell.viaduct.syntax.surface

import io.github.aplcornell.viaduct.prettyprinting.AnsiBaseColor
import io.github.aplcornell.viaduct.prettyprinting.AnsiColor
import io.github.aplcornell.viaduct.prettyprinting.Document
import io.github.aplcornell.viaduct.prettyprinting.NormalColor
import io.github.aplcornell.viaduct.prettyprinting.Style
import io.github.aplcornell.viaduct.prettyprinting.styled

/** The display style used for the keyword in the language. */
object KeywordStyle : Style {
    override val foregroundColor: AnsiColor
        get() = NormalColor(AnsiBaseColor.GREEN)
}

/** Converts [keyword] into a [Document] and applies [KeywordStyle] to it. */
internal fun keyword(keyword: String): Document =
    Document(keyword).styled(KeywordStyle)
