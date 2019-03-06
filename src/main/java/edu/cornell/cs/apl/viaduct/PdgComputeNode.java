package edu.cornell.cs.apl.viaduct;

import edu.cornell.cs.apl.viaduct.imp.ast.AstNode;

/** PDG compute node, which represents expressions or statements. */
public class PdgComputeNode extends PdgNode {
  boolean isDowngrade;

  /** constructor for non-downgrade nodes. */
  public PdgComputeNode(AstNode astNode, AbstractLineNumber lineno, Label label) {
    super(astNode, lineno);
    this.setInLabel(label);
    this.setOutLabel(label);
    this.isDowngrade = false;
  }

  /** constructor for downgrade nodes. */
  public PdgComputeNode(AstNode astNode, AbstractLineNumber lineno, Label inLabel, Label outLabel) {
    super(astNode, lineno);
    this.setInLabel(inLabel);
    this.setOutLabel(outLabel);
    this.isDowngrade = true;
  }

  @Override
  public boolean isStorageNode() {
    return false;
  }

  @Override
  public boolean isComputeNode() {
    return true;
  }

  @Override
  public boolean isDowngradeNode() {
    return this.isDowngrade;
  }

  @Override
  public boolean isControlNode() {
    return false;
  }

  @Override
  public String toString() {
    if (this.isDowngrade) {
      return "<"
          + this.lineNumber.toString()
          + " downgrade compute node for "
          + this.astNode.toString()
          + ">";

    } else {
      return "<"
          + this.lineNumber.toString()
          + " compute node for "
          + this.astNode.toString()
          + ">";
    }
  }
}
