package edu.cornell.cs.apl.viaduct;

import java.util.ArrayList;
import java.util.List;

/** builder for statements, implicitly creates a sequence through a fluent interface. */
public class StmtBuilder {
  List<StmtNode> stmts;

  public StmtBuilder() {
    this.stmts = new ArrayList<StmtNode>();
  }

  public StmtNode build() {
    return new SeqNode(this.stmts);
  }

  public StmtBuilder skip() {
    this.stmts.add(new SkipNode());
    return this;
  }

  public StmtBuilder varDecl(String varName, Label label) {
    this.stmts.add(new VarDeclNode(new Variable(varName), label));
    return this;
  }

  public StmtBuilder assign(String varName, ExprNode rhs) {
    this.stmts.add(new AssignNode(new Variable(varName), rhs));
    return this;
  }

  /** creates conditional/if nodes. */
  public StmtBuilder cond(ExprNode guard, StmtBuilder thenBranch, StmtBuilder elseBranch) {
    StmtNode ifNode = new IfNode(guard, thenBranch.build(), elseBranch.build());
    this.stmts.add(ifNode);
    return this;
  }
}
