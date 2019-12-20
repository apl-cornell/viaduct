package edu.cornell.cs.apl.viaduct.protocol;

import edu.cornell.cs.apl.viaduct.AstNode;
import edu.cornell.cs.apl.viaduct.UnknownProtocolException;
import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.pdg.PdgNode;
import edu.cornell.cs.apl.viaduct.pdg.ProgramDependencyGraph;
import io.vavr.collection.Map;
import java.util.Set;

public interface ProtocolSearchStrategy<T extends AstNode> {
  Set<Protocol<T>> createProtocolInstances(
      HostTrustConfiguration hostConfig,
      Map<PdgNode<T>, Protocol<T>> currProtoMap,
      PdgNode<T> node);

  int estimatePdgCost(Map<PdgNode<T>, Protocol<T>> protocolMap, ProgramDependencyGraph<T> pdg)
      throws UnknownProtocolException;
}
