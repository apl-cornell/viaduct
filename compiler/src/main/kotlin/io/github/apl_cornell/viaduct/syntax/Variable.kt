package io.github.apl_cornell.viaduct.syntax

import io.github.apl_cornell.viaduct.prettyprinting.AnsiBaseColor
import io.github.apl_cornell.viaduct.prettyprinting.AnsiColor
import io.github.apl_cornell.viaduct.prettyprinting.Document
import io.github.apl_cornell.viaduct.prettyprinting.NormalColor
import io.github.apl_cornell.viaduct.prettyprinting.Style
import io.github.apl_cornell.viaduct.prettyprinting.styled

/** A variable is a name that stands for a value or an object instance. */
open class Variable(override val name: String) : Name {
    override val nameCategory: String
        get() = "variable"

    override fun toDocument(): Document = Document(name).styled(VariableStyle)
}

/**
 * A variable that binds base values.
 *
 * Temporaries are generated internally to name expression results.
 */
data class Temporary(override val name: String) : Variable(name) {
    override val nameCategory: String
        get() = "temporary"
}

/** A variable that binds an object. */
data class ObjectVariable(override val name: String) : Variable(name) {
    override val nameCategory: String
        get() = "variable"
}

/** The display style of [Variable]s. */
object VariableStyle : Style {
    override val foregroundColor: AnsiColor
        get() = NormalColor(AnsiBaseColor.MAGENTA)
}
