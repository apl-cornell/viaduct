package edu.cornell.cs.apl.viaduct.imp;

import edu.cornell.cs.apl.viaduct.CompilationException;
import edu.cornell.cs.apl.viaduct.imp.ast.Host;

public class DuplicateHostDeclarationException extends CompilationException {
  public DuplicateHostDeclarationException(Host host) {
    super("Multiple trust declarations for host: " + host);
  }
}
