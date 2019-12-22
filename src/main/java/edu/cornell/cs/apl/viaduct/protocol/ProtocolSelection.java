package edu.cornell.cs.apl.viaduct.protocol;

import edu.cornell.cs.apl.viaduct.AstNode;
import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.pdg.PdgNode;
import edu.cornell.cs.apl.viaduct.pdg.ProgramDependencyGraph;
import io.vavr.collection.Map;

public interface ProtocolSelection<T extends AstNode> {
  Map<PdgNode<T>, Protocol<T>> selectProtocols(
      HostTrustConfiguration hostConfig, ProgramDependencyGraph<T> pdg);
}
