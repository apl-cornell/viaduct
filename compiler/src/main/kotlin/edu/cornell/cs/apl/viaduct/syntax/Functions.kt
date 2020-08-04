package edu.cornell.cs.apl.viaduct.syntax

import edu.cornell.cs.apl.prettyprinting.AnsiBaseColor
import edu.cornell.cs.apl.prettyprinting.AnsiColor
import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.NormalColor
import edu.cornell.cs.apl.prettyprinting.PrettyPrintable
import edu.cornell.cs.apl.prettyprinting.Style
import edu.cornell.cs.apl.prettyprinting.styled
import edu.cornell.cs.apl.viaduct.syntax.surface.KeywordStyle

/** Specifies whether a parameter is an IN parameter (can be used)
 *  or an OUT (has to be assigned in the function body).
 */
enum class ParameterType : PrettyPrintable {
    PARAM_IN {
        override val asDocument: Document = Document("")
    },
    PARAM_OUT {
        override val asDocument: Document = Document(" out").styled(KeywordStyle)
    }
}

/** The name of a parameter in a function declaration. */
data class ParameterName(override val name: String) : Name {
    override val nameCategory: String
        get() = "parameter"

    override val asDocument: Document
        get() = Document(name).styled(ParameterNameStyle)
}

object ParameterNameStyle : Style {
    override val foregroundColor: AnsiColor
        get() = NormalColor(AnsiBaseColor.MAGENTA)
}

/** The name of a function. */
data class FunctionName(override val name: String) : Name {
    override val nameCategory: String
        get() = "function"

    override val asDocument: Document
        get() = Document(name).styled(FunctionNameStyle)
}

object FunctionNameStyle : Style
