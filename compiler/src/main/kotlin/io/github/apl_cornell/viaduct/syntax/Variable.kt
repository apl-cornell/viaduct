package io.github.apl_cornell.viaduct.syntax

import io.github.apl_cornell.apl.prettyprinting.AnsiBaseColor
import io.github.apl_cornell.apl.prettyprinting.AnsiColor
import io.github.apl_cornell.apl.prettyprinting.Document
import io.github.apl_cornell.apl.prettyprinting.NormalColor
import io.github.apl_cornell.apl.prettyprinting.Style
import io.github.apl_cornell.apl.prettyprinting.styled

/** A variable is a name that stands for a value or an object instance. */
sealed class Variable : Name {
    override fun toDocument(): Document = Document(name).styled(VariableStyle)
}

/**
 * A variable that binds base values.
 *
 * Temporaries are generated internally to name expression results.
 */
data class Temporary(override val name: String) : Variable() {
    override val nameCategory: String
        get() = "temporary"
}

/** A variable that binds an object. */
data class ObjectVariable(override val name: String) : Variable() {
    override val nameCategory: String
        get() = "variable"
}

/** The display style of [Variable]s. */
object VariableStyle : Style {
    override val foregroundColor: AnsiColor
        get() = NormalColor(AnsiBaseColor.MAGENTA)
}
