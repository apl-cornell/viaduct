package edu.cornell.cs.apl.viaduct.syntax.datatypes

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.Style
import edu.cornell.cs.apl.prettyprinting.styled
import edu.cornell.cs.apl.viaduct.syntax.Name

/** The name of a primitive or user-defined class. */
data class ClassName(override val name: String) : Name {
    override val nameCategory: String
        get() = "class"

    override val asDocument: Document
        get() = Document(name).styled(ClassNameStyle)
}

/** The display style of [ClassName]s. */
object ClassNameStyle : Style
