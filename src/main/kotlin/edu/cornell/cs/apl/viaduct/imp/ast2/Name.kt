package edu.cornell.cs.apl.viaduct.imp.ast2

/** Objects that name things.
 *
 * @property name the given name
 * */
abstract class Name(val name: String) : Located() {
    /** Class of things this objects names. */
    abstract val nameCategory: String
}
