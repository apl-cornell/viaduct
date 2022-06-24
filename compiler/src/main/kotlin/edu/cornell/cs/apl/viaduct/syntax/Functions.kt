package edu.cornell.cs.apl.viaduct.syntax

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.PrettyPrintable
import edu.cornell.cs.apl.prettyprinting.Style
import edu.cornell.cs.apl.prettyprinting.styled
import edu.cornell.cs.apl.viaduct.syntax.surface.KeywordStyle

/**
 * Specifies whether a parameter is an IN parameter (can be used)
 * or an OUT (has to be assigned in the function body).
 */
enum class ParameterDirection : PrettyPrintable {
    IN {
        override fun toDocument(): Document = Document("")
    },
    OUT {
        override fun toDocument(): Document = Document(" out").styled(KeywordStyle)
    }
}

/** The name of a function. */
data class FunctionName(override val name: String) : Name {
    override val nameCategory: String
        get() = "function"

    override fun toDocument(): Document = Document(name).styled(FunctionNameStyle)
}

object FunctionNameStyle : Style
