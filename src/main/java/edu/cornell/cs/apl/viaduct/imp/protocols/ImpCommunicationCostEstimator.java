package edu.cornell.cs.apl.viaduct.imp.protocols;

import edu.cornell.cs.apl.viaduct.InvalidProtocolException;
import edu.cornell.cs.apl.viaduct.UnknownProtocolException;
import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.AssignNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BinaryExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BinaryOperators;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.HostName;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.pdg.PdgInfoEdge;
import edu.cornell.cs.apl.viaduct.pdg.PdgNode;
import edu.cornell.cs.apl.viaduct.pdg.ProgramDependencyGraph;
import edu.cornell.cs.apl.viaduct.protocol.Protocol;
import edu.cornell.cs.apl.viaduct.protocol.ProtocolCommunicationStrategy;
import edu.cornell.cs.apl.viaduct.protocol.ProtocolCostEstimator;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** model cost as the communication complexity of a protocol. */
public class ImpCommunicationCostEstimator extends ProtocolCostEstimator<ImpAstNode> {

  private static final int COMMUNICATION_COST = 1;
  private static final int BASE_STORAGE_COST = 0;

  private final HostTrustConfiguration hostConfig;
  private final ProtocolCommunicationStrategy<ImpAstNode> communicationStrategy;

  public ImpCommunicationCostEstimator(HostTrustConfiguration hostConfig,
      ProtocolCommunicationStrategy<ImpAstNode> communicationStrategy) {
    this.hostConfig = hostConfig;
    this.communicationStrategy = communicationStrategy;
  }

  private int estimateSingleCost(
      Map<PdgNode<ImpAstNode>, Protocol<ImpAstNode>> protocolMap,
      PdgNode<ImpAstNode> node,
      Single protocol) {

    HostName host = ((Single) protocol).getActualHost();
    if (node.isStorageNode()) {
      return BASE_STORAGE_COST;

    } else if (node.isComputeNode()) {
      // compute communication costs
      int numCommunications = 0;

      for (PdgInfoEdge<ImpAstNode> infoEdge : node.getReadEdges()) {
        Protocol<ImpAstNode> srcProto = protocolMap.get(infoEdge.getSource());
        Set<HostName> readSet = new HashSet<>(
            this.communicationStrategy.getReadSet(this.hostConfig, srcProto, protocol, host));
        readSet.remove(host);
        numCommunications += readSet.size();
      }

      for (PdgInfoEdge<ImpAstNode> infoEdge : node.getWriteEdges()) {
        Protocol<ImpAstNode> dstProto = protocolMap.get(infoEdge.getTarget());
        Set<HostName> writeSet = new HashSet<>(
            this.communicationStrategy.getReadSet(this.hostConfig, protocol, dstProto, host));
        writeSet.remove(host);
        numCommunications += writeSet.size();
      }

      return numCommunications * COMMUNICATION_COST;

    } else {
      // punt on costs of control nodes for now
      return 0;
    }
  }

  private int estimateReplicationCost(Map<PdgNode<ImpAstNode>, Protocol<ImpAstNode>> protocolMap,
      PdgNode<ImpAstNode> node, Replication protocol) {
    Replication replProto = (Replication) protocol;
    if (node.isStorageNode()) {
      return replProto.getHosts().size() * BASE_STORAGE_COST;

    } else if (node.isComputeNode()) {
      // compute communication costs
      final Set<HostName> hosts = protocol.getHosts();
      int numCommunications = 0;
      for (PdgInfoEdge<ImpAstNode> infoEdge : node.getReadEdges()) {
        final Protocol<ImpAstNode> srcProto = protocolMap.get(infoEdge.getSource());

        for (HostName host : hosts) {
          final Set<HostName> readSet = new HashSet<>(
              this.communicationStrategy.getReadSet(hostConfig, srcProto, protocol, host));
          readSet.remove(host);
          numCommunications += readSet.size();
        }
      }

      for (PdgInfoEdge<ImpAstNode> infoEdge : node.getWriteEdges()) {
        Protocol<ImpAstNode> dstProto = protocolMap.get(infoEdge.getTarget());

        for (HostName host : hosts) {
          final Set<HostName> writeSet = new HashSet<>(
              this.communicationStrategy.getReadSet(hostConfig, protocol, dstProto, host));
          writeSet.remove(host);
          numCommunications += writeSet.size();
        }
      }

      return numCommunications * COMMUNICATION_COST;

    } else {
      return 0;
    }
  }

  private int estimateZKCost(
      Map<PdgNode<ImpAstNode>,
      Protocol<ImpAstNode>> protocolMap,
      PdgNode<ImpAstNode> node,
      ZK protocol) {
    // TODO: we're not doing ZK yet
    return 0;
  }

  private int estimateMpcCost(
      Map<PdgNode<ImpAstNode>,
      Protocol<ImpAstNode>> protocolMap,
      PdgNode<ImpAstNode> node,
      MPC protocol) {

    HostName host = protocol.getActualHost();
    int partySize = protocol.getHosts().size();

    if (node.isStorageNode()) {
      return BASE_STORAGE_COST;

    } else if (node.isComputeNode()) {
      // compute communication costs
      int numCommunications = 0;

      for (PdgInfoEdge<ImpAstNode> infoEdge : node.getReadEdges()) {
        Protocol<ImpAstNode> srcProto = protocolMap.get(infoEdge.getSource());
        Set<HostName> readSet = new HashSet<>(
            this.communicationStrategy.getReadSet(this.hostConfig, srcProto, protocol, host));
        readSet.remove(host);
        numCommunications += readSet.size();
      }

      for (PdgInfoEdge<ImpAstNode> infoEdge : node.getWriteEdges()) {
        Protocol<ImpAstNode> dstProto = protocolMap.get(infoEdge.getTarget());
        Set<HostName> writeSet = new HashSet<>(
            this.communicationStrategy.getReadSet(this.hostConfig, protocol, dstProto, host));
        writeSet.remove(host);
        numCommunications += writeSet.size();
      }

      ImpAstNode astNode = node.getAstNode();
      ExpressionNode expr = null;
      if (astNode instanceof AssignNode) {
        expr = ((AssignNode) astNode).getRhs();

      } else if (astNode instanceof ExpressionNode) {
        expr = (ExpressionNode) astNode;
      }

      if (expr != null) {
        // multiplication induces n^2 communication
        if (expr instanceof BinaryExpressionNode) {
          BinaryExpressionNode binOpExpr = (BinaryExpressionNode) expr;
          if (binOpExpr.getOperator() instanceof BinaryOperators.Times) {
            numCommunications += (partySize * partySize);
          }
        }

        return numCommunications * COMMUNICATION_COST;

      } else {
        throw new Error("Compute node not associated with expression");
      }

    } else {
      return 0;
    }
  }

  @Override
  public int estimateNodeCost(
      PdgNode<ImpAstNode> node,
      Map<PdgNode<ImpAstNode>,
      Protocol<ImpAstNode>> protocolMap,
      ProgramDependencyGraph<ImpAstNode> pdg)
      throws UnknownProtocolException, InvalidProtocolException {

    try {
      Protocol<ImpAstNode> protocol = protocolMap.get(node);
      if (protocol instanceof Single) {
        return estimateSingleCost(protocolMap, node, (Single) protocol);

      } else if (protocol instanceof Replication) {
        return estimateReplicationCost(protocolMap, node, (Replication) protocol);

      } else if (protocol instanceof ZK) {
        return estimateZKCost(protocolMap, node, (ZK) protocol);

      } else if (protocol instanceof MPC) {
        return estimateMpcCost(protocolMap, node, (MPC) protocol);

      } else if (protocol instanceof ControlProtocol) {
        return 0;

      } else {
        throw new UnknownProtocolException(protocol);
      }

    } catch (InvalidProtocolException invalidProto)  {
      throw new InvalidProtocolException(node, invalidProto.getProtocol());
    }
  }
}
