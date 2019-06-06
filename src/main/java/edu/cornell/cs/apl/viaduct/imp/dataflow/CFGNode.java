package edu.cornell.cs.apl.viaduct.imp.dataflow;

import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** control flow graph node. */
public class CFGNode {
  StmtNode statement;
  Set<CFGNode> inNodes;
  Set<CFGNode> outNodes;

  /** constructor. */
  public CFGNode(StmtNode s) {
    this.inNodes = new HashSet<>();
    this.outNodes = new HashSet<>();
    this.statement = s;
  }

  public StmtNode getStatement() {
    return this.statement;
  }

  public Set<CFGNode> getInNodes() {
    return this.inNodes;
  }

  public Set<CFGNode> getOutNodes() {
    return this.outNodes;
  }

  public void addInNode(CFGNode node) {
    this.inNodes.add(node);
  }

  public void addOutNode(CFGNode node) {
    this.outNodes.add(node);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof CFGNode)) {
      return false;
    }

    final CFGNode that = (CFGNode) o;
    return Objects.equals(this.statement, that.statement);
  }

  @Override
  public int hashCode() {
    return this.statement.hashCode();
  }

  @Override
  public String toString() {
    return String.format("CFG node: %s", this.statement);
  }
}
