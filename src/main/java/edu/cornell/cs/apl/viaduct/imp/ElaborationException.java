package edu.cornell.cs.apl.viaduct.imp;

import edu.cornell.cs.apl.viaduct.CompilationException;

public class ElaborationException extends CompilationException {
  public ElaborationException() {
    super("Derived forms not elaborated!");
  }
}
