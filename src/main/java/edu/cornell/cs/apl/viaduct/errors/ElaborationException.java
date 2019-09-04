package edu.cornell.cs.apl.viaduct.errors;

/**
 * This exception is raised when the compiler encounters an AST node that should have been removed
 * in a previous pass.
 *
 * <p>Indicates a programming error.
 */
public class ElaborationException extends RuntimeException {
  public ElaborationException() {
    super("Derived forms not elaborated!");
  }
}
