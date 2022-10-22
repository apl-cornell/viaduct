package io.github.aplcornell.viaduct.syntax.values

import io.github.aplcornell.viaduct.prettyprinting.AnsiBaseColor
import io.github.aplcornell.viaduct.prettyprinting.AnsiColor
import io.github.aplcornell.viaduct.prettyprinting.Document
import io.github.aplcornell.viaduct.prettyprinting.NormalColor
import io.github.aplcornell.viaduct.prettyprinting.PrettyPrintable
import io.github.aplcornell.viaduct.prettyprinting.Style
import io.github.aplcornell.viaduct.prettyprinting.styled
import io.github.aplcornell.viaduct.syntax.types.ValueType

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
