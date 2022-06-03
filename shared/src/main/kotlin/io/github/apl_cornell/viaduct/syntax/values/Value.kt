package io.github.apl_cornell.viaduct.syntax.values

import io.github.apl_cornell.apl.prettyprinting.AnsiBaseColor
import io.github.apl_cornell.apl.prettyprinting.AnsiColor
import io.github.apl_cornell.apl.prettyprinting.Document
import io.github.apl_cornell.apl.prettyprinting.NormalColor
import io.github.apl_cornell.apl.prettyprinting.PrettyPrintable
import io.github.apl_cornell.apl.prettyprinting.Style
import io.github.apl_cornell.apl.prettyprinting.styled
import io.github.apl_cornell.viaduct.syntax.types.ValueType

/** The result of evaluating an expression. */
abstract class Value : PrettyPrintable {
    /** The type of the value. */
    abstract val type: ValueType

    final override fun toDocument(): Document = Document(this.toString()).styled(ValueStyle)
}

/** The display style of [Value]s. */
object ValueStyle : Style {
    override val foregroundColor: AnsiColor =
        NormalColor(AnsiBaseColor.CYAN)
}
