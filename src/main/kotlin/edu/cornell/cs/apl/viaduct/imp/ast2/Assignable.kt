package edu.cornell.cs.apl.viaduct.imp.ast2

class Assignable(name: String) : Name(name) {
    override val nameCategory: String
        get() = "assignable"
}
