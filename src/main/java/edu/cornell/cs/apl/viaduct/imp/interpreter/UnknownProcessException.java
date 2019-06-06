package edu.cornell.cs.apl.viaduct.imp.interpreter;

import edu.cornell.cs.apl.viaduct.imp.ast.ProcessName;

class UnknownProcessException extends Exception {
  UnknownProcessException(ProcessName processName) {
    super("Unknown process: " + processName);
  }
}
