package edu.cornell.cs.apl.viaduct;

import edu.cornell.cs.apl.viaduct.surface.AssignNode;
import edu.cornell.cs.apl.viaduct.surface.BlockNode;
import edu.cornell.cs.apl.viaduct.surface.ExpressionNode;
import edu.cornell.cs.apl.viaduct.surface.IfNode;
import edu.cornell.cs.apl.viaduct.surface.SkipNode;
import edu.cornell.cs.apl.viaduct.surface.StmtNode;
import edu.cornell.cs.apl.viaduct.surface.VarDeclNode;
import edu.cornell.cs.apl.viaduct.surface.Variable;
import java.util.ArrayList;
import java.util.List;

/** builder for statements, implicitly creates a sequence through a fluent interface. */
public class StmtBuilder {
  List<StmtNode> stmts;

  public StmtBuilder() {
    this.stmts = new ArrayList<StmtNode>();
  }

  public StmtNode build() {
    return new BlockNode(this.stmts);
  }

  public StmtBuilder skip() {
    this.stmts.add(new SkipNode());
    return this;
  }

  public StmtBuilder varDecl(String varName, Label label) {
    this.stmts.add(new VarDeclNode(new Variable(varName), label));
    return this;
  }

  public StmtBuilder assign(String varName, ExpressionNode rhs) {
    this.stmts.add(new AssignNode(new Variable(varName), rhs));
    return this;
  }

  /** creates conditional/if nodes. */
  public StmtBuilder cond(ExpressionNode guard, StmtBuilder thenBranch, StmtBuilder elseBranch) {
    StmtNode ifNode = new IfNode(guard, thenBranch.build(), elseBranch.build());
    this.stmts.add(ifNode);
    return this;
  }
}
