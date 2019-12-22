package edu.cornell.cs.apl.viaduct.imp.protocols;

import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.HostName;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.pdg.PdgNode;
import edu.cornell.cs.apl.viaduct.protocol.PairProtocolFactory;
import edu.cornell.cs.apl.viaduct.protocol.Protocol;
import io.vavr.Tuple2;
import io.vavr.collection.Map;

/** contains ZK information flow constraints. */
public class ZKFactory extends PairProtocolFactory<ImpAstNode> {
  @Override
  protected Protocol<ImpAstNode> createInstanceFromHostInfo(
      PdgNode<ImpAstNode> node,
      Map<PdgNode<ImpAstNode>, Protocol<ImpAstNode>> protoMap,
      HostTrustConfiguration hostConfig,
      Tuple2<HostName, HostName> hostPair) {
    if (node.isComputeNode()) {
      return new ZK(hostConfig, hostPair._1(), hostPair._2());

    } else {
      return null;
    }
  }
}
