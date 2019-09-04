package edu.cornell.cs.apl.viaduct.imp.visitors;

/** An unhandled case in a visitor. */
public class MissingCaseError extends RuntimeException {
  // TODO: replace Object with ImpAstNode once ReferenceNode is a subclass of ImpAstNode.
  public MissingCaseError(Object node) {
    super("Visitor does not handle the case for " + node);
  }
}
