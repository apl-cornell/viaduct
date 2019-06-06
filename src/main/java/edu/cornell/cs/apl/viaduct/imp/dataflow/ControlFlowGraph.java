package edu.cornell.cs.apl.viaduct.imp.dataflow;

import edu.cornell.cs.apl.viaduct.imp.ast.Variable;

import java.util.List;
import java.util.Set;

/** control flow graph. */
public class ControlFlowGraph {
  List<CFGNode> nodes;
  Set<Variable> declaredVars;
  Set<Variable> tempVars;
  List<Variable> vars;

  /** constructor. */
  public ControlFlowGraph(List<CFGNode> ns,
      Set<Variable> dvs, Set<Variable> tvs, List<Variable> vs) {

    this.nodes = ns;
    this.declaredVars = dvs;
    this.tempVars = tvs;
    this.vars = vs;
  }

  public List<CFGNode> getNodes() {
    return this.nodes;
  }

  public Set<Variable> getDeclaredVars() {
    return this.declaredVars;
  }

  public Set<Variable> getTempVars() {
    return this.tempVars;
  }

  public List<Variable> getVars() {
    return this.vars;
  }
}
