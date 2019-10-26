package edu.cornell.cs.apl.viaduct.protocol;

import edu.cornell.cs.apl.viaduct.AstNode;
import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessName;
import io.vavr.Tuple2;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** memoize protocol communication strategy into a map. */
public abstract class MemoizedProtocolCommunicationStrategy<T extends AstNode>
    implements ProtocolCommunicationStrategy<T> {
  private final Map<Tuple2<Protocol<T>, Protocol<T>>, Map<ProcessName, Set<ProcessName>>>
      communicationMap;

  public MemoizedProtocolCommunicationStrategy() {
    this.communicationMap = new HashMap<>();
  }

  protected abstract Map<ProcessName, Set<ProcessName>> computeCommunicationMap(
      HostTrustConfiguration hostConfig, Protocol<T> fromProtocol, Protocol<T> toProtocol);

  protected Map<ProcessName, Set<ProcessName>> getCommunicationMap(
      HostTrustConfiguration hostConfig, Protocol<T> fromProtocol, Protocol<T> toProtocol) {

    Tuple2<Protocol<T>, Protocol<T>> protocolPair = new Tuple2<>(fromProtocol, toProtocol);
    if (this.communicationMap.containsKey(protocolPair)) {
      return this.communicationMap.get(protocolPair);

    } else {
      Map<ProcessName, Set<ProcessName>> communication =
          computeCommunicationMap(hostConfig, fromProtocol, toProtocol);
      this.communicationMap.put(protocolPair, communication);
      return communication;
    }
  }
}
