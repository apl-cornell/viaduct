package edu.cornell.cs.apl.viaduct.dataflow;

import edu.cornell.cs.apl.viaduct.AstNode;
import edu.cornell.cs.apl.viaduct.pdg.PdgInfoEdge;
import edu.cornell.cs.apl.viaduct.pdg.PdgNode;
import edu.cornell.cs.apl.viaduct.security.Label;

/** dataflow analysis to compute the confidentiality requirements of PDG nodes.
 * note that starting from the top of the trust lattice (weakest principal)
 * and going down (meet) until fixpoint, the analysis computes the weakest
 * principal necessary to perform the computation / store the variable
 * represented by the PDG node.
 * confidentiality analysis is a forward analysis that goes UP the IF lattice.
 */
public class ConfidentialityDataflow<T extends AstNode>
    extends PdgLabelDataflow<T> {

  public ConfidentialityDataflow() {
    super(DataflowType.FORWARD, DataflowDirection.UP);
  }

  @Override
  protected boolean includeInfoEdge(PdgInfoEdge<T> edge) {
    return true;
  }

  @Override
  protected void updateInput(PdgNode<T> node, Label nextInput) {
    // Label curLabel = node.getInLabel();
    // node.setInLabel(nextInput.confidentiality().and(curLabel.integrity()));

    node.setInLabel(nextInput.confidentiality());
  }

  @Override
  protected void updateOutput(PdgNode<T> node, Label nextOutput) {
    // Label curLabel = node.getOutLabel();
    // node.setOutLabel(nextOutput.confidentiality().and(curLabel.integrity()));

    node.setOutLabel(nextOutput);
  }

  @Override
  protected Label transfer(PdgNode<T> node, Label next) {
    // if the node is a downgrade node, prevent transfer;
    // the out label is permanently the downgrade label
    if (node.isDowngradeNode()) {
      return node.getOutLabel();
    } else {
      // Label cur = node.getOutLabel();
      // return next.confidentiality().and(cur.integrity());

      return next;
    }
  }
}
