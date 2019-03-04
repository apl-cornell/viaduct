package edu.cornell.cs.apl.viaduct;

/** PDG node for control flow statements. */
public class PdgControlNode extends PdgNode {
  public PdgControlNode(AstNode astNode, AbstractLineNumber lineno, Label label) {
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
    return "<" + this.lineNumber.toString() + " control node for "
            + this.astNode.toString() + ">";
  }
}