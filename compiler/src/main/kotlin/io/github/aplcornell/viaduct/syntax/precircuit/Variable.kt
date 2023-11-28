package io.github.aplcornell.viaduct.syntax.precircuit

import io.github.aplcornell.viaduct.prettyprinting.Document
import io.github.aplcornell.viaduct.prettyprinting.styled
import io.github.aplcornell.viaduct.syntax.Located
import io.github.aplcornell.viaduct.syntax.Name
import io.github.aplcornell.viaduct.syntax.VariableStyle

/** A variable is a name that stands for a value or an object instance. */
data class Variable(override val name: String) : Name {
    override val nameCategory: String
        get() = "variable"

    override fun toDocument(): Document = Document(name).styled(VariableStyle)
}

typealias VariableNode = Located<Variable>
