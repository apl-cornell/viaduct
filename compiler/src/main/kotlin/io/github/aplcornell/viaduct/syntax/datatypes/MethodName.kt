package io.github.aplcornell.viaduct.syntax.datatypes

import io.github.aplcornell.viaduct.prettyprinting.AnsiBaseColor
import io.github.aplcornell.viaduct.prettyprinting.AnsiColor
import io.github.aplcornell.viaduct.prettyprinting.BrightColor
import io.github.aplcornell.viaduct.prettyprinting.Document
import io.github.aplcornell.viaduct.prettyprinting.Style
import io.github.aplcornell.viaduct.prettyprinting.styled
import io.github.aplcornell.viaduct.syntax.Name

/** An object method. */
interface MethodName : Name {
    override val nameCategory: String
        get() = "method"

    override fun toDocument(): Document = Document(name).styled(MethodNameStyle)
}

/** The display style of [MethodName]s. */
object MethodNameStyle : Style {
    override val foregroundColor: AnsiColor
        get() = BrightColor(AnsiBaseColor.MAGENTA)
}
