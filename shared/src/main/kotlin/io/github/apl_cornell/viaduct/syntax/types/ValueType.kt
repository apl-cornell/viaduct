package io.github.apl_cornell.viaduct.syntax.types

import io.github.apl_cornell.apl.prettyprinting.AnsiBaseColor
import io.github.apl_cornell.apl.prettyprinting.AnsiColor
import io.github.apl_cornell.apl.prettyprinting.Document
import io.github.apl_cornell.apl.prettyprinting.NormalColor
import io.github.apl_cornell.apl.prettyprinting.Style
import io.github.apl_cornell.apl.prettyprinting.styled
import io.github.apl_cornell.viaduct.syntax.values.Value

/**
 * The type of a [Value].
 *
 * Data types such as arrays are not value types.
 */
abstract class ValueType : Type {
    /** The default value of this type. */
    abstract val defaultValue: Value

    final override fun toDocument(): Document = Document(this.toString()).styled(ValueTypeStyle)
}

/** The display style of [ValueType]s. */
object ValueTypeStyle : Style {
    override val foregroundColor: AnsiColor =
        NormalColor(AnsiBaseColor.YELLOW)
}
