package edu.cornell.cs.apl.viaduct;

/** PDG storage node, which represents declared variables. */
public class PdgStorageNode extends PdgNode {
  Label label;

  public PdgStorageNode(AstNode astNode, Label label) {
    super(astNode);
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
  public String toString() {
    return "<storage node for " + this.astNode.toString() + ">";
  }
}
