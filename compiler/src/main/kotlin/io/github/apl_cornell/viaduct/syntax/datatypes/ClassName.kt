package io.github.apl_cornell.viaduct.syntax.datatypes

import io.github.apl_cornell.viaduct.prettyprinting.Document
import io.github.apl_cornell.viaduct.prettyprinting.Style
import io.github.apl_cornell.viaduct.prettyprinting.styled
import io.github.apl_cornell.viaduct.syntax.Name

/** The name of a primitive or user-defined class. */
data class ClassName(override val name: String) : Name {
    override val nameCategory: String
        get() = "class"

    override fun toDocument(): Document = Document(name).styled(ClassNameStyle)
}

/** The display style of [ClassName]s. */
object ClassNameStyle : Style
