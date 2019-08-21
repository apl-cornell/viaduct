package edu.cornell.cs.apl.viaduct.imp.protocols;

import edu.cornell.cs.apl.viaduct.UnknownProtocolException;
import edu.cornell.cs.apl.viaduct.imp.ast.AssignNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BinaryExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BinaryOperators;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Host;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.imp.protocols.ControlProtocol;
import edu.cornell.cs.apl.viaduct.imp.protocols.MPC;
import edu.cornell.cs.apl.viaduct.imp.protocols.Replication;
import edu.cornell.cs.apl.viaduct.imp.protocols.Single;
import edu.cornell.cs.apl.viaduct.imp.protocols.ZK;
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
public class ImpCommunicationCostEstimator
    extends ProtocolCostEstimator<ImpAstNode> {

  private static final int COMMUNICATION_COST = 1;
  private static final int BASE_STORAGE_COST = 0;

  private final ProtocolCommunicationStrategy<ImpAstNode> communicationStrategy;

  public ImpCommunicationCostEstimator(
      ProtocolCommunicationStrategy<ImpAstNode> communicationStrategy)
  {
    this.communicationStrategy = communicationStrategy;
  }

  private int estimateSingleCost(
      Map<PdgNode<ImpAstNode>,Protocol<ImpAstNode>> protocolMap,
      PdgNode<ImpAstNode> node,
      Single protocol)
  {
    Host host = ((Single)protocol).getHost();
    if (node.isStorageNode()) {
      return BASE_STORAGE_COST;

    } else if (node.isComputeNode()) {
      // compute communication costs
      Set<Host> communicatingHosts = new HashSet<>();

      for (PdgInfoEdge<ImpAstNode> infoEdge : node.getReadEdges()) {
        Protocol<ImpAstNode> srcProto = protocolMap.get(infoEdge.getSource());
        communicatingHosts.addAll(srcProto.getHosts());
      }

      for (PdgInfoEdge<ImpAstNode> infoEdge : node.getWriteEdges()) {
        Protocol<ImpAstNode> srcProto = protocolMap.get(infoEdge.getTarget());
        communicatingHosts.addAll(srcProto.getHosts());
      }

      communicatingHosts.remove(host);
      int commHostSize = communicatingHosts.size();
      return commHostSize * COMMUNICATION_COST;

    } else {
      // punt on costs of control nodes for now
      return 0;
    }
  }

  private int estimateReplicationCost(
      Map<PdgNode<ImpAstNode>,Protocol<ImpAstNode>> protocolMap,
      PdgNode<ImpAstNode> node,
      Replication protocol)
  {
    Replication replProto = (Replication) protocol;
    if (node.isStorageNode()) {
      return replProto.getRealReplicas().size() * BASE_STORAGE_COST;

    } else if (node.isComputeNode()) {
      // compute communication costs
      Set<Host> communicatingHosts = new HashSet<>();

      for (PdgInfoEdge<ImpAstNode> infoEdge : node.getReadEdges()) {
        Protocol<ImpAstNode> srcProto = protocolMap.get(infoEdge.getSource());
        communicatingHosts.addAll(srcProto.getHosts());
      }

      for (PdgInfoEdge<ImpAstNode> infoEdge : node.getWriteEdges()) {
        Protocol<ImpAstNode> srcProto = protocolMap.get(infoEdge.getTarget());
        communicatingHosts.addAll(srcProto.getHosts());
      }

      int sum = 0;
      Set<Host> hosts = replProto.getHosts();
      int commHostSize = communicatingHosts.size();
      for (Host host : hosts) {
        if (communicatingHosts.contains(host)) {
          sum += (commHostSize - 1) * COMMUNICATION_COST;

        } else {
          sum += commHostSize * COMMUNICATION_COST;
        }
      }
      return sum;

    } else {
      return 0;
    }
  }

  private int estimateZKCost(
      Map<PdgNode<ImpAstNode>,Protocol<ImpAstNode>> protocolMap,
      PdgNode<ImpAstNode> node,
      ZK protocol)
  {
    // TODO: we're not doing ZK yet
    return 0;
  }

  private int estimateMpcCost(
      Map<PdgNode<ImpAstNode>,Protocol<ImpAstNode>> protocolMap,
      PdgNode<ImpAstNode> node,
      MPC protocol)
  {
    MPC mpcProto = (MPC)protocol;
    Host mpcHost = mpcProto.getHost();
    int partySize = mpcProto.getParties().size();

    if (node.isStorageNode()) {
      return BASE_STORAGE_COST;

    } else if (node.isComputeNode()) {
      // compute communication costs
      Set<Host> communicatingHosts = new HashSet<>();

      for (PdgInfoEdge<ImpAstNode> infoEdge : node.getReadEdges()) {
        Protocol<ImpAstNode> srcProto = protocolMap.get(infoEdge.getSource());
        communicatingHosts.addAll(srcProto.getHosts());
      }

      for (PdgInfoEdge<ImpAstNode> infoEdge : node.getWriteEdges()) {
        Protocol<ImpAstNode> srcProto = protocolMap.get(infoEdge.getTarget());
        communicatingHosts.addAll(srcProto.getHosts());
      }

      communicatingHosts.remove(mpcHost);
      int commHostSize = communicatingHosts.size();
      int cost = commHostSize * COMMUNICATION_COST;

      ImpAstNode astNode = node.getAstNode();
      ExpressionNode expr = null;
      if (astNode instanceof AssignNode) {
        expr = ((AssignNode)astNode).getRhs();

      } else if (astNode instanceof ExpressionNode) {
        expr = (ExpressionNode)astNode;
      }

      if (expr != null) {
        // multiplication induces n^2 communication
        if (expr instanceof BinaryExpressionNode) {
          BinaryExpressionNode binOpExpr = (BinaryExpressionNode)expr;
          if (binOpExpr.getOperator() instanceof BinaryOperators.Times) {
            return cost + (partySize * partySize * COMMUNICATION_COST);
          }
        }

        return cost;

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
      Map<PdgNode<ImpAstNode>, Protocol<ImpAstNode>> protocolMap,
      ProgramDependencyGraph<ImpAstNode> pdg)
      throws UnknownProtocolException {

    Protocol<ImpAstNode> protocol = protocolMap.get(node);
    if (protocol instanceof Single) {
      return estimateSingleCost(protocolMap, node, (Single)protocol);

    } else if (protocol instanceof Replication) {
      return estimateReplicationCost(protocolMap, node, (Replication)protocol);

    } else if (protocol instanceof ZK) {
      return estimateZKCost(protocolMap, node, (ZK)protocol);

    } else if (protocol instanceof MPC) {
      return estimateMpcCost(protocolMap, node, (MPC)protocol);

    } else if (protocol instanceof ControlProtocol) {
      return 0;

    } else {
      throw new UnknownProtocolException(protocol);
    }
  }
}
