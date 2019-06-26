package edu.cornell.cs.apl.viaduct.imp;

import edu.cornell.cs.apl.viaduct.CompilationException;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessName;

public class DuplicateProcessDefinitionException extends CompilationException {
  public DuplicateProcessDefinitionException(ProcessName processName) {
    super("Multiple definitions for process: " + processName);
  }
}
