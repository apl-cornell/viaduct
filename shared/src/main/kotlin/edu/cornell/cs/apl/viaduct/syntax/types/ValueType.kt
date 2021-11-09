package edu.cornell.cs.apl.viaduct.syntax.types

import edu.cornell.cs.apl.prettyprinting.AnsiBaseColor
import edu.cornell.cs.apl.prettyprinting.AnsiColor
import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.NormalColor
import edu.cornell.cs.apl.prettyprinting.Style
import edu.cornell.cs.apl.prettyprinting.styled
import edu.cornell.cs.apl.viaduct.syntax.values.Value

/**
 * The type of a [Value].
 *
 * Data types such as arrays are not value types.
 */
abstract class ValueType : Type {
    /** The default value of this type. */
    abstract val defaultValue: Value

    final override fun asDocument(): Document = Document(this.toString()).styled(ValueTypeStyle)
}

/** The display style of [ValueType]s. */
object ValueTypeStyle : Style {
    override val foregroundColor: AnsiColor =
        NormalColor(AnsiBaseColor.YELLOW)
}
