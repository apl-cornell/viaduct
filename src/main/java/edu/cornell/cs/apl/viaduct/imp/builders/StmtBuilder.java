package edu.cornell.cs.apl.viaduct.imp.builders;

import edu.cornell.cs.apl.viaduct.Host;
import edu.cornell.cs.apl.viaduct.ProgramDependencyGraph.ControlLabel;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/** Builder for statements. Implicitly creates a sequence through a fluent interface. */
public class StmtBuilder {
  private abstract static class ControlInfo {
    final List<StmtNode> prefix;
    final Map<ControlLabel,StmtNode> pathMap;
    ControlLabel currentPath;

    public ControlInfo(List<StmtNode> pref) {
      this.prefix = pref;
      this.pathMap = new HashMap<>();
      this.currentPath = null;
    }

    public void setCurrentPath(ControlLabel label) {
      this.currentPath = label;
    }

    public void finishCurrentPath(List<StmtNode> pathStmts) {
      this.pathMap.put(this.currentPath, new BlockNode(pathStmts));
      this.currentPath = null;
    }

    public List<StmtNode> pop() {
      StmtNode controlStructure = buildControlStructure();
      this.prefix.add(controlStructure);
      return this.prefix;
    }

    public abstract StmtNode buildControlStructure();
  }

  private static class ConditionalControlInfo extends ControlInfo {
    final ExpressionNode guard;

    public ConditionalControlInfo(List<StmtNode> pref, ExpressionNode g) {
      super(pref);
      this.guard = g;
    }

    public StmtNode buildControlStructure() {
      StmtNode thenBranch = this.pathMap.get(ControlLabel.THEN);
      thenBranch = thenBranch != null ? thenBranch : new SkipNode();

      StmtNode elseBranch = this.pathMap.get(ControlLabel.ELSE);
      elseBranch = elseBranch != null ? elseBranch : new SkipNode();

      return new IfNode(this.guard, thenBranch, elseBranch);
    }
  }

  private final Stack<ControlInfo> controlContext;
  private List<StmtNode> stmts;

  public StmtBuilder() {
    this.controlContext = new Stack<>();
    this.stmts = new ArrayList<>();
  }

  /** push a conditional into the builder control context. */
  public StmtBuilder pushIf(ExpressionNode guard) {
    ControlInfo execPath = new ConditionalControlInfo(this.stmts, guard);
    this.controlContext.push(execPath);
    this.stmts = new ArrayList<>();
    return this;
  }

  /** set the execution path of the current control structure. */
  public StmtBuilder setCurrentPath(ControlLabel label)  {
    this.controlContext.peek().setCurrentPath(label);
    return this;
  }

  /** finish execution path of current control structure. */
  public StmtBuilder finishCurrentPath() {
    this.controlContext.peek().finishCurrentPath(this.stmts);
    this.stmts = new ArrayList<>();
    return this;
  }

  /** pop top structure from control context and build it. */
  public StmtBuilder popControl() {
    ControlInfo controlInfo = this.controlContext.pop();
    this.stmts = controlInfo.pop();
    return this;
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
