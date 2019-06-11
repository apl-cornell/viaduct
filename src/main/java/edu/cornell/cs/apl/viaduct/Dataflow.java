package edu.cornell.cs.apl.viaduct;

import edu.cornell.cs.apl.viaduct.security.Lattice;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/** generic dataflow class using the worklist algorithm. */
public abstract class Dataflow<T extends Lattice<T>, N> {
  public enum DataflowType { FORWARD, BACKWARD }

  protected DataflowType type;

  protected Dataflow(DataflowType dt) {
    this.type = dt;
  }

  protected abstract T input(N node);

  protected abstract T output(N node);

  protected abstract T transfer(N node, T nextInput);

  protected abstract void updateInput(N node, T nextInput);

  protected abstract void updateOutput(N node, T nextInput);

  protected abstract Set<N> getInNodes(N node);

  protected abstract Set<N> getOutNodes(N node);

  /** worklist algorithm for dataflow analysis. */
  public void dataflow(List<N> nodes) {
    if (this.type == DataflowType.FORWARD) {
      dataflowForward(nodes);

    } else {
      dataflowBackward(nodes);
    }
  }

  private void dataflowForward(List<N> nodes) {
    Queue<N> worklist = new LinkedList<N>(nodes);

    while (worklist.size() > 0) {
      N node = worklist.remove();
      T nextInput = input(node);

      for (N inNode : getInNodes(node)) {
        nextInput = nextInput.join(output(inNode));
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

  /** worklist algorithm for backwards dataflow analysis. */
  private void dataflowBackward(List<N> nodes) {
    Collections.reverse(nodes);
    Queue<N> worklist = new LinkedList<N>(nodes);

    while (worklist.size() > 0) {
      N node = worklist.remove();
      T nextOutput = output(node);
      for (N outNode : getOutNodes(node)) {
        nextOutput = nextOutput.join(input(outNode));
      }
      updateOutput(node, nextOutput);

      T curInput = input(node);
      T nextInput = transfer(node, nextOutput);

      if (!nextInput.equals(curInput)) {
        updateInput(node, nextInput);
        worklist.add(node);
      }
    }
  }
}
