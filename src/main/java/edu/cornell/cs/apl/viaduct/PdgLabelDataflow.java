package edu.cornell.cs.apl.viaduct;

import edu.cornell.cs.apl.viaduct.security.Label;

import java.util.HashSet;
import java.util.Set;

public abstract class PdgLabelDataflow<T extends AstNode>
    extends Dataflow<Label, PdgNode<T>> {

  public PdgLabelDataflow(DataflowType type) {
    super(type);
  }

  @Override
  protected Set<PdgNode<T>> getInNodes(PdgNode<T> node) {
    Set<PdgNode<T>> inNodes = new HashSet<>();
    for (PdgEdge<T> inEdge : node.getInInfoEdges()) {
      inNodes.add(inEdge.getSource());
    }

    return inNodes;
  }

  @Override
  protected Set<PdgNode<T>> getOutNodes(PdgNode<T> node)  {
    Set<PdgNode<T>> outNodes = new HashSet<>();
    for (PdgEdge<T> outEdge : node.getOutInfoEdges()) {
      outNodes.add(outEdge.getTarget());
    }

    return outNodes;
  }

  @Override
  protected Label input(PdgNode<T> node) {
    return node.getInLabel();
  }

  @Override
  protected Label output(PdgNode<T> node) {
    return node.getOutLabel();
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
