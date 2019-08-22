package edu.cornell.cs.apl.viaduct.imp.parser;

/** Classes that have a source location. */
public interface Located {
  SourceRange getSourceLocation();
}
