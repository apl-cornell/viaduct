package edu.cornell.cs.apl.viaduct;

import edu.cornell.cs.apl.viaduct.security.Label;

/** PDG node for control flow statements. */
public class PdgControlNode<T extends AstNode> extends PdgNode<T> {
  /** constructor that sets in and out labels to be the same. */
  public PdgControlNode(T astNode, AbstractLineNumber lineno, Label label) {
    super(astNode, lineno);
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

  @Override
  public String toString() {
    return "<" + this.lineNumber.toString() + " control node for " + this.astNode.toString() + ">";
  }
}
