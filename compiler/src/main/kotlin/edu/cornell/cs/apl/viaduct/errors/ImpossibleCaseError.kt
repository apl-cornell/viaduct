package edu.cornell.cs.apl.viaduct.errors

/** Thrown when the compiler reaches an impossible state. */
abstract class ImpossibleCaseError : CompilationError() {
    override val category = "Impossible Case"
}
