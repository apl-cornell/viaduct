package edu.cornell.cs.apl.viaduct.syntax.types

import edu.cornell.cs.apl.prettyprinting.AnsiBaseColor
import edu.cornell.cs.apl.prettyprinting.AnsiColor
import edu.cornell.cs.apl.prettyprinting.NormalColor
import edu.cornell.cs.apl.prettyprinting.PrettyPrintable
import edu.cornell.cs.apl.prettyprinting.Style
import edu.cornell.cs.apl.viaduct.syntax.values.Value

/**
 * The type of a [Value].
 *
 * Data types such as arrays are not value types.
 */
interface ValueType : Type, PrettyPrintable {
    /** The default value of this type. */
    val defaultValue: Value
}

/** The display style of [ValueType]s. */
object ValueTypeStyle : Style {
    override val foregroundColor: AnsiColor =
        NormalColor(AnsiBaseColor.YELLOW)
}
