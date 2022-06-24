package edu.cornell.cs.apl.viaduct.syntax.values

import edu.cornell.cs.apl.prettyprinting.AnsiBaseColor
import edu.cornell.cs.apl.prettyprinting.AnsiColor
import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.NormalColor
import edu.cornell.cs.apl.prettyprinting.PrettyPrintable
import edu.cornell.cs.apl.prettyprinting.Style
import edu.cornell.cs.apl.prettyprinting.styled
import edu.cornell.cs.apl.viaduct.syntax.types.ValueType

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
