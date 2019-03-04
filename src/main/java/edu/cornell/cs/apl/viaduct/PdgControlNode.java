package edu.cornell.cs.apl.viaduct;

/** PDG node for control flow statements. */
public class PdgControlNode extends PdgNode {
  Label label;

  public PdgControlNode(AstNode astNode, AbstractLineNumber lineno, Label label) {
    super(astNode, lineno);
    this.label = label;
  }

  @Override
  public Label getLabel() {
    return this.label;
  }

  @Override
  public void setLabel(Label label) {
    this.label = label;
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