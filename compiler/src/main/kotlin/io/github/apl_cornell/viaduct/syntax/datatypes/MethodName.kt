package io.github.apl_cornell.viaduct.syntax.datatypes

import io.github.apl_cornell.apl.prettyprinting.AnsiBaseColor
import io.github.apl_cornell.apl.prettyprinting.AnsiColor
import io.github.apl_cornell.apl.prettyprinting.BrightColor
import io.github.apl_cornell.apl.prettyprinting.Document
import io.github.apl_cornell.apl.prettyprinting.Style
import io.github.apl_cornell.apl.prettyprinting.styled
import io.github.apl_cornell.viaduct.syntax.Name

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
