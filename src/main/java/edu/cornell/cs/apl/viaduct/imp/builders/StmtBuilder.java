package edu.cornell.cs.apl.viaduct.imp.builders;

import edu.cornell.cs.apl.viaduct.Host;
import edu.cornell.cs.apl.viaduct.imp.ast.AnnotationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssignNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.DeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.IfNode;
import edu.cornell.cs.apl.viaduct.imp.ast.RecvNode;
import edu.cornell.cs.apl.viaduct.imp.ast.SendNode;
import edu.cornell.cs.apl.viaduct.imp.ast.SkipNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.security.Label;
import java.util.ArrayList;
import java.util.List;

/** Builder for statements. Implicitly creates a sequence through a fluent interface. */
public class StmtBuilder {
  private final List<StmtNode> stmts;

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
    this.stmts.add(new DeclarationNode(new Variable(varName), label));
    return this;
  }

  public StmtBuilder varDecl(Variable varName, Label label) {
    this.stmts.add(new DeclarationNode(varName, label));
    return this;
  }

  public StmtBuilder assign(String varName, ExpressionNode rhs) {
    this.stmts.add(new AssignNode(new Variable(varName), rhs));
    return this;
  }

  public StmtBuilder assign(Variable varName, ExpressionNode rhs) {
    this.stmts.add(new AssignNode(varName, rhs));
    return this;
  }

  /** Creates conditional/if nodes. */
  public StmtBuilder cond(ExpressionNode guard, StmtBuilder thenBranch, StmtBuilder elseBranch) {
    StmtNode ifNode = new IfNode(guard, thenBranch.build(), elseBranch.build());
    this.stmts.add(ifNode);
    return this;
  }

  /** build send stmt. */
  public StmtBuilder send(String recipient, ExpressionNode expr) {
    StmtNode sendNode = new SendNode(new Host(recipient), expr);
    this.stmts.add(sendNode);
    return this;
  }

  /** build send stmt. */
  public StmtBuilder send(Host recipient, ExpressionNode expr) {
    StmtNode sendNode = new SendNode(recipient, expr);
    this.stmts.add(sendNode);
    return this;
  }

  /** build recv stmt. */
  public StmtBuilder recv(String sender, String var) {
    StmtNode recvNode = new RecvNode(new Host(sender), new Variable(var));
    this.stmts.add(recvNode);
    return this;
  }

  /** build recv stmt. */
  public StmtBuilder recv(Host sender, Variable var) {
    StmtNode recvNode = new RecvNode(sender, var);
    this.stmts.add(recvNode);
    return this;
  }

  /** build annotation. */
  public StmtBuilder annotation(String annotStr) {
    StmtNode annotNode = new AnnotationNode(annotStr);
    this.stmts.add(annotNode);
    return this;
  }

  public StmtBuilder statement(StmtNode stmt) {
    this.stmts.add(stmt);
    return this;
  }

  public StmtBuilder concat(StmtBuilder other) {
    this.stmts.addAll(other.stmts);
    return this;
  }
}
