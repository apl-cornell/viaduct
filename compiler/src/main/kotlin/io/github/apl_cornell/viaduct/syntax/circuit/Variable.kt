package io.github.apl_cornell.viaduct.syntax.circuit

import io.github.apl_cornell.viaduct.prettyprinting.Document
import io.github.apl_cornell.viaduct.prettyprinting.styled
import io.github.apl_cornell.viaduct.syntax.Located
import io.github.apl_cornell.viaduct.syntax.Name
import io.github.apl_cornell.viaduct.syntax.VariableStyle

/** A variable is a name that stands for a value or an object instance. */
data class Variable(override val name: String) : Name {
    override val nameCategory: String
        get() = "variable"

    override fun toDocument(): Document = Document(name).styled(VariableStyle)
}

typealias VariableNode = Located<Variable>
