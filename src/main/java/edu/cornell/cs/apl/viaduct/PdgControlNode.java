package edu.cornell.cs.apl.viaduct;

import edu.cornell.cs.apl.viaduct.ProgramDependencyGraph.ControlEdgeComparator;
import edu.cornell.cs.apl.viaduct.ProgramDependencyGraph.ControlLabel;
import edu.cornell.cs.apl.viaduct.security.Label;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** PDG node for control flow statements. */
public class PdgControlNode<T extends AstNode> extends PdgNode<T> {
  /** constructor that sets in and out labels to be the same. */
  public PdgControlNode(ProgramDependencyGraph<T> pdg, T astNode, String id, Label label) {
    super(pdg, astNode, id);
    this.setInLabel(label);
    this.setOutLabel(label);
  }

  @Override
  public boolean isStorageNode() {
    return false;
  }

  @Override
  public boolean isComputeNode() {
    return false;
  }

  @Override
  public boolean isDowngradeNode() {
    return false;
  }

  @Override
  public boolean isControlNode() {
    return true;
  }

  /** get nodes associated with the control structure.
   * e.g. if the control node represents an if statement,
   * this returns all the nodes (in order) in the branches */
  public List<PdgNode<T>> getControlStructureNodes() {
    List<PdgControlEdge<T>> pathStarts = new ArrayList<>();
    for (PdgControlEdge<T> controlEdge : getOutControlEdges()) {
      if (controlEdge.getLabel() != ControlLabel.SEQ) {
        pathStarts.add(controlEdge);
      }
    }

    ControlEdgeComparator edgeComparator = new ControlEdgeComparator();
    Collections.sort(pathStarts, edgeComparator);

    List<PdgNode<T>> nodeList = new ArrayList<>();
    for (PdgControlEdge<T> pathStart : pathStarts) {
      PdgNode<T> pathStartNode = pathStart.getTarget();
      List<PdgNode<T>> pathNodes = this.pdg.getOrderedNodesFrom(pathStartNode);
      nodeList.addAll(pathNodes);
    }

    return nodeList;
  }

  @Override
  public String toString() {
    return "<" + this.id.toString() + " control node for " + this.astNode.toString() + ">";
  }
}
