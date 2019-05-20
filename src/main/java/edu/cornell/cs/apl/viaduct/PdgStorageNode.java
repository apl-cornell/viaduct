package edu.cornell.cs.apl.viaduct;

import edu.cornell.cs.apl.viaduct.imp.ast.AstNode;
import edu.cornell.cs.apl.viaduct.security.Label;

/** PDG storage node, which represents declared variables. */
public class PdgStorageNode<T extends AstNode> extends PdgNode<T> {
  /** constructor that sets in and out labels to be the same. */
  public PdgStorageNode(T astNode, AbstractLineNumber lineno, Label label) {
    super(astNode, lineno);
    this.setInLabel(label);
    this.setOutLabel(label);
  }

  @Override
  public boolean isStorageNode() {
    return true;
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
    return false;
  }

  @Override
  public String toString() {
    return "<" + this.lineNumber.toString() + " storage node for " + this.astNode.toString() + ">";
  }
}
