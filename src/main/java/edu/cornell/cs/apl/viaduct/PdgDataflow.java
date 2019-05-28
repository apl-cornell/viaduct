package edu.cornell.cs.apl.viaduct;

import edu.cornell.cs.apl.viaduct.security.Lattice;
import java.util.LinkedList;
import java.util.Queue;

/** dataflow analysis for PDGs. parameterized by lattice defined by subclasses. */
public abstract class PdgDataflow<T extends Lattice<T>, U extends AstNode> {
  protected abstract T input(PdgNode<U> node);

  protected abstract T output(PdgNode<U> node);

  protected abstract T transfer(PdgNode<U> node, T nextInput);

  protected abstract void updateInput(PdgNode<U> node, T nextInput);

  protected abstract void updateOutput(PdgNode<U> node, T nextInput);

  /** worklist algorithm. */
  public void dataflow(ProgramDependencyGraph<U> pdg) {
    Queue<PdgNode<U>> worklist = new LinkedList<PdgNode<U>>(pdg.getNodes());

    while (worklist.size() > 0) {
      PdgNode<U> node = worklist.remove();

      T nextInput = input(node);
      for (PdgEdge<U> inEdge : node.getInInfoEdges()) {
        nextInput = nextInput.join(output(inEdge.getSource()));
      }
      updateInput(node, nextInput);

      T curOutput = output(node);
      T nextOutput = transfer(node, nextInput);

      // if output has been updated, add node back to the worklist
      if (!nextOutput.equals(curOutput)) {
        updateOutput(node, nextOutput);
        worklist.add(node);
      }
    }
  }
}
