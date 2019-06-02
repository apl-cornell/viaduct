package edu.cornell.cs.apl.viaduct;

import edu.cornell.cs.apl.viaduct.security.Label;

/** PDG compute node, which represents expressions or statements. */
public class PdgComputeNode<T extends AstNode> extends PdgNode<T> {
  boolean isDowngrade;

  /** constructor for non-downgrade nodes. */
  public PdgComputeNode(ProgramDependencyGraph<T> pdg, T astNode, String id, Label label) {
    super(pdg, astNode, id);
    this.setInLabel(label);
    this.setOutLabel(label);
    this.isDowngrade = false;
  }

  /** constructor for downgrade nodes. */
  public PdgComputeNode(ProgramDependencyGraph<T> pdg, T astNode, String id,
      Label inLabel, Label outLabel) {

    super(pdg, astNode, id);
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
          + this.id.toString()
          + " downgrade compute node for "
          + this.astNode.toString()
          + ">";

    } else {
      return "<" + this.id.toString() + " compute node for " + this.astNode.toString() + ">";
    }
  }
}
