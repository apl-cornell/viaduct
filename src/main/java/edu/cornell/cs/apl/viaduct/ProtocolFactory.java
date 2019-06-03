package edu.cornell.cs.apl.viaduct;

import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.AstNode;

import java.util.Map;
import java.util.Set;

/** determines whether to create protocol instances during protocol selection. */
public interface ProtocolFactory<T extends AstNode> {
  Set<Protocol<T>> createInstances(HostTrustConfiguration hostConfig,
      Map<PdgNode<T>, Protocol<T>> currProtoMap, PdgNode<T> node);
}
