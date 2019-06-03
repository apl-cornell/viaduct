package edu.cornell.cs.apl.viaduct.imp.interpreter;

import edu.cornell.cs.apl.viaduct.imp.ast.Host;

class UnknownHostException extends Exception {
  UnknownHostException(Host host) {
    super("Unknown host: " + host);
  }
}
