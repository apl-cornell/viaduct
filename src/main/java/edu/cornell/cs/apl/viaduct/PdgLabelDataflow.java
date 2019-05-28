package edu.cornell.cs.apl.viaduct;

import edu.cornell.cs.apl.viaduct.security.Label;

public class PdgLabelDataflow<T extends AstNode> extends PdgDataflow<Label, T> {
  @Override
  protected Label input(PdgNode<T> node) {
    return node.getInLabel();
  }

  @Override
  protected Label output(PdgNode<T> node) {
    return node.getOutLabel();
  }

  @Override
  protected Label transfer(PdgNode<T> node, Label nextInput) {
    // if the node is a downgrade node, prevent transfer;
    // the out label is permanently the downgrade label
    if (node.isDowngradeNode()) {
      return node.getOutLabel();
    } else {
      return nextInput;
    }
  }

  @Override
  protected void updateInput(PdgNode<T> node, Label nextInput) {
    node.setInLabel(nextInput);
  }

  @Override
  protected void updateOutput(PdgNode<T> node, Label nextOutput) {
    node.setOutLabel(nextOutput);
  }
}
