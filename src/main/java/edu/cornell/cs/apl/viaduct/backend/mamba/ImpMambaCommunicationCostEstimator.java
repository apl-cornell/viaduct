package edu.cornell.cs.apl.viaduct.backend.mamba;

import edu.cornell.cs.apl.viaduct.InvalidProtocolException;
import edu.cornell.cs.apl.viaduct.UnknownProtocolException;
import edu.cornell.cs.apl.viaduct.backend.mamba.protocols.MambaPublic;
import edu.cornell.cs.apl.viaduct.backend.mamba.protocols.MambaSecret;
import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.AssignNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BinaryExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BinaryOperators;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessName;
import edu.cornell.cs.apl.viaduct.imp.protocols.ImpCommunicationCostEstimator;
import edu.cornell.cs.apl.viaduct.pdg.PdgInfoEdge;
import edu.cornell.cs.apl.viaduct.pdg.PdgNode;
import edu.cornell.cs.apl.viaduct.pdg.ProgramDependencyGraph;
import edu.cornell.cs.apl.viaduct.protocol.Protocol;
import edu.cornell.cs.apl.viaduct.protocol.ProtocolCommunicationStrategy;

import io.vavr.collection.Map;

import java.util.HashSet;
import java.util.Set;

public class ImpMambaCommunicationCostEstimator extends ImpCommunicationCostEstimator {
  private static int PUBLIC_COST = 100;
  private static int SECRET_COST = 1000;

  public ImpMambaCommunicationCostEstimator(
      HostTrustConfiguration hostConfig,
      ProtocolCommunicationStrategy<ImpAstNode> communicationStrategy)
  {
    super(hostConfig, communicationStrategy);
  }

  // copied directly from estimateReplicationCost
  protected int estimateMambaPublicCost(
      Map<PdgNode<ImpAstNode>, Protocol<ImpAstNode>> protocolMap,
      PdgNode<ImpAstNode> node,
      MambaPublic protocol)
  {
    if (node.isStorageNode()) {
      return PUBLIC_COST;

    } else if (node.isComputeNode()) {
      // compute communication costs
      final Set<ProcessName> processes = protocol.getProcesses();
      int numCommunications = 0;
      for (PdgInfoEdge<ImpAstNode> infoEdge : node.getReadEdges()) {
        final Protocol<ImpAstNode> srcProto = protocolMap.getOrElse(infoEdge.getSource(), null);

        for (ProcessName process : processes) {
          final Set<ProcessName> readSet = new HashSet<>(
              this.communicationStrategy.getReadSet(hostConfig, srcProto, protocol, process));
          readSet.remove(process);
          numCommunications += readSet.size();
        }
      }

      for (PdgInfoEdge<ImpAstNode> infoEdge : node.getWriteEdges()) {
        Protocol<ImpAstNode> dstProto = protocolMap.getOrElse(infoEdge.getTarget(), null);

        for (ProcessName process : processes) {
          final Set<ProcessName> writeSet = new HashSet<>(
              this.communicationStrategy.getReadSet(hostConfig, protocol, dstProto, process));
          writeSet.remove(process);
          numCommunications += writeSet.size();
        }
      }

      return PUBLIC_COST + (numCommunications * COMMUNICATION_COST);

    } else {
      return 0;
    }

  }

  // copied directly from estimateMpcCost
  protected int estimateMambaSecretCost(
      Map<PdgNode<ImpAstNode>, Protocol<ImpAstNode>> protocolMap,
      PdgNode<ImpAstNode> node,
      MambaSecret protocol)
  {
    ProcessName process = protocol.getProcess();
    int partySize = protocol.getHosts().size();

    if (node.isStorageNode()) {
      return SECRET_COST;

    } else if (node.isComputeNode()) {
      // compute communication costs
      int numCommunications = 0;

      for (PdgInfoEdge<ImpAstNode> infoEdge : node.getReadEdges()) {
        Protocol<ImpAstNode> srcProto = protocolMap.getOrElse(infoEdge.getSource(), null);
        Set<ProcessName> readSet = new HashSet<>(
            this.communicationStrategy.getReadSet(this.hostConfig, srcProto, protocol, process));
        readSet.remove(process);
        numCommunications += readSet.size();
      }

      for (PdgInfoEdge<ImpAstNode> infoEdge : node.getWriteEdges()) {
        Protocol<ImpAstNode> dstProto = protocolMap.getOrElse(infoEdge.getTarget(), null);
        Set<ProcessName> writeSet = new HashSet<>(
            this.communicationStrategy.getReadSet(this.hostConfig, protocol, dstProto, process));
        writeSet.remove(process);
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

        return SECRET_COST + (numCommunications * COMMUNICATION_COST);

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
      throws UnknownProtocolException, InvalidProtocolException
  {
    try {
      Protocol<ImpAstNode> protocol = protocolMap.getOrElse(node, null);
      if (protocol instanceof MambaPublic) {
        return estimateMambaPublicCost(protocolMap, node, (MambaPublic) protocol);

      } else if (protocol instanceof MambaSecret) {
        return estimateMambaSecretCost(protocolMap, node, (MambaSecret) protocol);
      }

    } catch (InvalidProtocolException invalidProto)  {
      throw new InvalidProtocolException(node, invalidProto.getProtocol());
    }

    return super.estimateNodeCost(node, protocolMap, pdg);
  }
}
