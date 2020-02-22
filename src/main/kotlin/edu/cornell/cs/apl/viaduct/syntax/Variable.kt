package edu.cornell.cs.apl.viaduct.syntax

import edu.cornell.cs.apl.prettyprinting.AnsiBaseColor
import edu.cornell.cs.apl.prettyprinting.AnsiColor
import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.NormalColor
import edu.cornell.cs.apl.prettyprinting.Style
import edu.cornell.cs.apl.prettyprinting.styled

/** A variable is a name that stands for a value or an object instance. */
sealed class Variable : Name {
    override val asDocument: Document
        get() = Document(name).styled(VariableStyle)
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
