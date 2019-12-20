package edu.cornell.cs.apl.viaduct.imp.ast2

/** A variable that binds base values.
 *
 * Temporaries are generated internally to name expression results.
 * */
class Temporary(name: String) : Name(name) {
    override val nameCategory: String
        get() = "temporary"
}
