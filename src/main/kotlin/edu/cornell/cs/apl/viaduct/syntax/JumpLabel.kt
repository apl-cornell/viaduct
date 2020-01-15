package edu.cornell.cs.apl.viaduct.syntax

/** The target label for unstructured control statements like `continue` and `break`. */
data class JumpLabel(override val name: String) : Name {
    override val nameCategory: String
        get() = "label"
}
