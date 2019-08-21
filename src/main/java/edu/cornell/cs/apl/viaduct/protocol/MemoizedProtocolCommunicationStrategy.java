package edu.cornell.cs.apl.viaduct.protocol;

import edu.cornell.cs.apl.viaduct.AstNode;
import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.Host;

import io.vavr.Tuple2;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** memoize protocol communication strategy into a map. */
public class MemoizedProtocolCommunicationStrategy<T extends AstNode>
    implements ProtocolCommunicationStrategy<T>
{
  private final Map<Tuple2<Protocol<T>,Protocol<T>>, Map<Host,Set<Host>>> communicationMap;
  private final ProtocolCommunicationStrategy<T> strategy;

  public MemoizedProtocolCommunicationStrategy(ProtocolCommunicationStrategy<T> strategy) {
    this.communicationMap = new HashMap<>();
    this.strategy = strategy;
  }

  @Override
  public Map<Host, Set<Host>> getCommunication(
      HostTrustConfiguration hostConfig,
      Protocol<T> fromProtocol, Protocol<T> toProtocol) {

    Tuple2<Protocol<T>,Protocol<T>> protocolPair = new Tuple2<>(fromProtocol,toProtocol);
    if (this.communicationMap.containsKey(protocolPair)) {
      return this.communicationMap.get(protocolPair);

    } else {
      Map<Host, Set<Host>> communication =
          this.strategy.getCommunication(hostConfig, fromProtocol, toProtocol);
      this.communicationMap.put(protocolPair, communication);
      return communication;
    }
  }
}
