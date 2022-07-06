package io.github.apl_cornell.viaduct.syntax

import io.github.apl_cornell.viaduct.prettyprinting.Document
import io.github.apl_cornell.viaduct.prettyprinting.PrettyPrintable
import io.github.apl_cornell.viaduct.prettyprinting.Style
import io.github.apl_cornell.viaduct.prettyprinting.styled
import io.github.apl_cornell.viaduct.syntax.surface.KeywordStyle

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
