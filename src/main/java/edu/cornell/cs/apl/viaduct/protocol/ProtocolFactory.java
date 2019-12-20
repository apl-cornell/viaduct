package edu.cornell.cs.apl.viaduct.protocol;

import edu.cornell.cs.apl.viaduct.AstNode;
import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.pdg.PdgNode;
import io.vavr.collection.Map;
import java.util.Set;

/** determines whether to create protocol instances during protocol selection. */
public interface ProtocolFactory<T extends AstNode> {
  Set<Protocol<T>> createInstances(
      HostTrustConfiguration hostConfig,
      Map<PdgNode<T>, Protocol<T>> currProtoMap,
      PdgNode<T> node);
}
