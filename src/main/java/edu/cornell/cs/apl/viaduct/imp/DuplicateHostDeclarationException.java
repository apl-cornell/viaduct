package edu.cornell.cs.apl.viaduct.imp;

import edu.cornell.cs.apl.viaduct.imp.ast.Host;

public class DuplicateHostDeclarationException extends Exception {
  public DuplicateHostDeclarationException(Host host) {
    super("Multiple trust declarations for host: " + host);
  }
}
