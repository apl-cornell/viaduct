package edu.cornell.cs.apl.viaduct.imp.parsing;

import edu.cornell.cs.apl.viaduct.util.AbstractLineNumber;

/** Classes that have a source location. */
public interface HasLocation {
  SourceRange getSourceLocation();

  AbstractLineNumber getLogicalPosition();
}
