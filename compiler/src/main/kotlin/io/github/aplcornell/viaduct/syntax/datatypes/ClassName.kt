package io.github.aplcornell.viaduct.syntax.datatypes

import io.github.aplcornell.viaduct.prettyprinting.Document
import io.github.aplcornell.viaduct.prettyprinting.Style
import io.github.aplcornell.viaduct.prettyprinting.styled
import io.github.aplcornell.viaduct.syntax.Name

/** The name of a primitive or user-defined class. */
data class ClassName(override val name: String) : Name {
    override val nameCategory: String
        get() = "class"

    override fun toDocument(): Document = Document(name).styled(ClassNameStyle)
}

/** The display style of [ClassName]s. */
object ClassNameStyle : Style
