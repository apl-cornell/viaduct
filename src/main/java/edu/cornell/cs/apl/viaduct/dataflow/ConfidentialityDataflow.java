package edu.cornell.cs.apl.viaduct.dataflow;

import edu.cornell.cs.apl.viaduct.AstNode;
import edu.cornell.cs.apl.viaduct.pdg.PdgNode;
import edu.cornell.cs.apl.viaduct.security.Label;

public class ConfidentialityDataflow<T extends AstNode>
    extends PdgLabelDataflow<T> {

  public ConfidentialityDataflow() {
    super(DataflowType.FORWARD);
  }

  @Override
  protected void updateInput(PdgNode<T> node, Label nextInput) {
    Label curLabel = node.getInLabel();
    node.setInLabel(nextInput.confidentiality().and(curLabel.integrity()));
  }

  @Override
  protected void updateOutput(PdgNode<T> node, Label nextOutput) {
    Label curLabel = node.getOutLabel();
    node.setOutLabel(nextOutput.confidentiality().and(curLabel.integrity()));
  }

  @Override
  protected Label transfer(PdgNode<T> node, Label next) {
    // if the node is a downgrade node, prevent transfer;
    // the out label is permanently the downgrade label
    if (node.isDowngradeNode()) {
      return node.getOutLabel();
    } else {
      Label cur = node.getOutLabel();
      return next.confidentiality().and(cur.integrity());
    }
  }
}
