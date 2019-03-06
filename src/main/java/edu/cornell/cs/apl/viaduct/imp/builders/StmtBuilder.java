package edu.cornell.cs.apl.viaduct.imp.builders;

import edu.cornell.cs.apl.viaduct.Label;
import edu.cornell.cs.apl.viaduct.imp.ast.AssignNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.IfNode;
import edu.cornell.cs.apl.viaduct.imp.ast.SkipNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;
import edu.cornell.cs.apl.viaduct.imp.ast.VarDeclNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import java.util.ArrayList;
import java.util.List;

/** Builder for statements. Implicitly creates a sequence through a fluent interface. */
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

  /** Creates conditional/if nodes. */
  public StmtBuilder cond(ExpressionNode guard, StmtBuilder thenBranch, StmtBuilder elseBranch) {
    StmtNode ifNode = new IfNode(guard, thenBranch.build(), elseBranch.build());
    this.stmts.add(ifNode);
    return this;
  }
}
