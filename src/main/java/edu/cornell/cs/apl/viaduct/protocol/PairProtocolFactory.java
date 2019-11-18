package edu.cornell.cs.apl.viaduct.protocol;

import edu.cornell.cs.apl.viaduct.AstNode;
import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.HostName;
import edu.cornell.cs.apl.viaduct.imp.protocols.LabelProtocolFactory;

import io.vavr.Tuple2;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class PairProtocolFactory<T extends AstNode>
    extends LabelProtocolFactory<T, Tuple2<HostName, HostName>> {
  @Override
  protected Iterable<Tuple2<HostName, HostName>> getHostInfo(HostTrustConfiguration hostConfig) {
    List<HostName> hostList = new ArrayList<>(hostConfig.hosts());
    Set<Tuple2<HostName,HostName>> hostPairs = new HashSet<>();

    for (int i = 0; i < hostList.size(); i++) {
      for (int j = 0; j < hostList.size(); j++) {
        if (i != j) {
          hostPairs.add(new Tuple2<>(hostList.get(i), hostList.get(j)));
        }
      }
    }
    return hostPairs;
  }
}
