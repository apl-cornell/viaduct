package edu.cornell.cs.apl.viaduct.syntax.datatypes

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.Style
import edu.cornell.cs.apl.prettyprinting.styled
import edu.cornell.cs.apl.viaduct.syntax.Name

/** An object method. */
interface MethodName : Name {
    override val nameCategory: String
        get() = "method"

    override val asDocument: Document
        get() = Document(name).styled(MethodNameStyle)
}

/** The display style of [MethodName]s. */
object MethodNameStyle : Style
