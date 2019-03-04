package edu.cornell.cs.apl.viaduct;

import java.util.LinkedList;
import java.util.Queue;

/** dataflow analysis for PDGs. parameterized by lattice defined by subclasses. */
public abstract class PdgDataflow<T extends Lattice<T>> {
  protected abstract T input(PdgNode node);
  protected abstract T output(PdgNode node);
  protected abstract T transfer(PdgNode node, T nextInput);
  protected abstract void update(PdgNode node, T input, T output);

  /** worklist algorithm */
  public void dataflow(ProgramDependencyGraph pdg) {
    Queue<PdgNode> worklist = new LinkedList<PdgNode>(pdg.getNodes());

    while (worklist.size() > 0) {
      PdgNode node = worklist.remove();

      T nextInput = input(node);
      for (PdgNode inNode : node.getInNodes())
      {
        nextInput = nextInput.join(output(inNode));
      }

      T curOutput = output(node);
      T nextOutput = transfer(node, nextInput);

      // if output has been updated, add node back to the worklist
      if (!nextOutput.equals(curOutput)) {
        update(node, nextInput, nextOutput);
        worklist.add(node);
      }
    }
  }
}