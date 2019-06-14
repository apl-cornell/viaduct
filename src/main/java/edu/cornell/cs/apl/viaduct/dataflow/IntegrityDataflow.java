package edu.cornell.cs.apl.viaduct.dataflow;

import edu.cornell.cs.apl.viaduct.AstNode;
import edu.cornell.cs.apl.viaduct.pdg.PdgInfoEdge;
import edu.cornell.cs.apl.viaduct.pdg.PdgNode;
import edu.cornell.cs.apl.viaduct.security.Label;

import java.util.HashSet;
import java.util.Set;

/** dataflow analysis to compute the integrity requirements of PDG nodes.
 * note that starting from the top of the trust lattice (weakest principal)
 * and going down (meet) until fixpoint, the analysis computes the weakest
 * principal necessary to perform the computation / store the variable
 * represented by the PDG node.
 * integrity analysis is a backwards analysis that goes DOWN the IF lattice
 */
public class IntegrityDataflow<T extends AstNode>
    extends PdgLabelDataflow<T> {

  public IntegrityDataflow() {
    super(DataflowType.BACKWARD, DataflowDirection.DOWN);
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
