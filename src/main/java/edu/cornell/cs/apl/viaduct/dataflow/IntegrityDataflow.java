package edu.cornell.cs.apl.viaduct.dataflow;

import edu.cornell.cs.apl.viaduct.AstNode;
import edu.cornell.cs.apl.viaduct.pdg.PdgInfoEdge;
import edu.cornell.cs.apl.viaduct.pdg.PdgNode;
import edu.cornell.cs.apl.viaduct.security.Label;

import java.util.HashSet;
import java.util.Set;

public class IntegrityDataflow<T extends AstNode>
    extends PdgLabelDataflow<T> {

  public IntegrityDataflow() {
    super(DataflowType.BACKWARD);
  }

  @Override
  protected Set<PdgNode<T>> getOutNodes(PdgNode<T> node)  {
    Set<PdgNode<T>> outNodes = new HashSet<>();
    for (PdgInfoEdge<T> outEdge : node.getOutInfoEdges()) {
      if (!outEdge.isFlowEdge()) {
        outNodes.add(outEdge.getTarget());
      }
    }

    return outNodes;
  }

  @Override
  protected void updateInput(PdgNode<T> node, Label nextInput) {
    Label curLabel = node.getInLabel();
    node.setInLabel(nextInput.integrity().and(curLabel.confidentiality()));
  }

  @Override
  protected void updateOutput(PdgNode<T> node, Label nextOutput) {
    Label curLabel = node.getOutLabel();
    node.setOutLabel(nextOutput.integrity().and(curLabel.confidentiality()));
  }

  @Override
  protected Label transfer(PdgNode<T> node, Label next) {
    // if the node is a downgrade node, prevent transfer;
    // the out label is permanently the downgrade label
    if (node.isDowngradeNode()) {
      return node.getInLabel();
    } else {
      Label cur = node.getInLabel();
      return next.integrity().and(cur.confidentiality());
    }
  }
}
