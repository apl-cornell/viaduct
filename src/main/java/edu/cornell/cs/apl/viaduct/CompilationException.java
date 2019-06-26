package edu.cornell.cs.apl.viaduct;

/**
 * Superclass of all exceptions caused by bad user input to the compiler.
 *
 * <p>Errors caused by bugs in the compiler do not belong here.
 */
public abstract class CompilationException extends Error {
  public CompilationException(String message) {
    super(message);
  }
}
