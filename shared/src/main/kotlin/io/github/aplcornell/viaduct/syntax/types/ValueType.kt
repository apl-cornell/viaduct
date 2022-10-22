package io.github.aplcornell.viaduct.syntax.types

import io.github.aplcornell.viaduct.prettyprinting.AnsiBaseColor
import io.github.aplcornell.viaduct.prettyprinting.AnsiColor
import io.github.aplcornell.viaduct.prettyprinting.Document
import io.github.aplcornell.viaduct.prettyprinting.NormalColor
import io.github.aplcornell.viaduct.prettyprinting.Style
import io.github.aplcornell.viaduct.prettyprinting.styled
import io.github.aplcornell.viaduct.syntax.values.Value

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
