package io.github.apl_cornell.viaduct.syntax

import io.github.apl_cornell.apl.prettyprinting.Document
import io.github.apl_cornell.apl.prettyprinting.PrettyPrintable
import io.github.apl_cornell.apl.prettyprinting.Style
import io.github.apl_cornell.apl.prettyprinting.styled
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

/**
 * Specifies whether a delegation is an information flow delegation
 * or an authority delegation.
 */
enum class DelegationKind : PrettyPrintable {
    IFC {
        override fun toDocument(): Document = Document("IFC Delegation")
    },
    AUTHORITY {
        override fun toDocument(): Document = Document("Authority Delegation")
    }
}

/**
 * Specifies whether a delegation is an information flow delegation
 * or an authority delegation.
 */
enum class DelegationProjection : PrettyPrintable {
    INTEGRITY {
        override fun toDocument(): Document = Document("Integrity")
    },
    CONFIDENTIALITY {
        override fun toDocument(): Document = Document("Confidentiality")
    },
    BOTH {
        override fun toDocument(): Document = Document("Both Projections")
    }
}
