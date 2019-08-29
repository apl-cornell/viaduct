package edu.cornell.cs.apl.viaduct.imp.builders;

import edu.cornell.cs.apl.viaduct.imp.ast.ArrayDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ArrayIndex;
import edu.cornell.cs.apl.viaduct.imp.ast.AssertNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssignNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BreakNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Host;
import edu.cornell.cs.apl.viaduct.imp.ast.IfNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpType;
import edu.cornell.cs.apl.viaduct.imp.ast.IntegerValue;
import edu.cornell.cs.apl.viaduct.imp.ast.LetBindingNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LiteralNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LoopNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessName;
import edu.cornell.cs.apl.viaduct.imp.ast.ReceiveNode;
import edu.cornell.cs.apl.viaduct.imp.ast.SendNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StatementNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.imp.ast.VariableDeclarationNode;
import edu.cornell.cs.apl.viaduct.pdg.ProgramDependencyGraph.ControlLabel;
import edu.cornell.cs.apl.viaduct.security.Label;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/** Builder for statements. Implicitly creates a sequence through a fluent interface. */
public class StmtBuilder {
  private final Stack<ControlInfo> controlContext;
  private List<StatementNode> stmts;

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

  /** push a loop into the builder control context. */
  public StmtBuilder pushLoop() {
    ControlInfo execPath = new LoopControlInfo(this.stmts);
    this.controlContext.push(execPath);
    this.stmts = new ArrayList<>();
    return this;
  }

  /** set the execution path of the current control structure. */
  public StmtBuilder setCurrentPath(ControlLabel label) {
    ControlInfo controlInfo = this.controlContext.peek();

    // hack; there could be statements added before the current path was set;
    // in which case, those statements belong to the prefix, not the path
    if (this.stmts.size() > 0) {
      controlInfo.prefix.addAll(this.stmts);
      this.stmts = new ArrayList<>();
    }

    controlInfo.setCurrentPath(label);
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

  public boolean isControlContextEmpty() {
    return this.controlContext.isEmpty();
  }

  public StatementNode build() {
    assert this.controlContext.empty();
    return BlockNode.create(this.stmts);
  }

  public StmtBuilder varDecl(String varName, ImpType type, Label label) {
    this.stmts.add(VariableDeclarationNode.create(Variable.create(varName), type, label));
    return this;
  }

  public StmtBuilder varDecl(Variable varName, ImpType type, Label label) {
    this.stmts.add(VariableDeclarationNode.create(varName, type, label));
    return this;
  }

  public StmtBuilder arrayDecl(String varName, ExpressionNode length, ImpType type, Label label) {
    this.stmts.add(ArrayDeclarationNode.create(Variable.create(varName), length, type, label));
    return this;
  }

  public StmtBuilder arrayDecl(Variable varName, ExpressionNode length, ImpType type, Label label) {
    this.stmts.add(ArrayDeclarationNode.create(varName, length, type, label));
    return this;
  }

  public StmtBuilder let(Variable varName, ExpressionNode rhs) {
    this.stmts.add(LetBindingNode.create(varName, rhs));
    return this;
  }

  public StmtBuilder assign(String varName, ExpressionNode rhs) {
    this.stmts.add(AssignNode.create(Variable.create(varName), rhs));
    return this;
  }

  public StmtBuilder assign(Variable varName, ExpressionNode rhs) {
    this.stmts.add(AssignNode.create(varName, rhs));
    return this;
  }

  public StmtBuilder assign(Variable arrName, ExpressionNode idx, ExpressionNode rhs) {
    this.stmts.add(AssignNode.create(ArrayIndex.create(arrName, idx), rhs));
    return this;
  }

  /** Creates conditional/if nodes. */
  public StmtBuilder cond(ExpressionNode guard, StmtBuilder thenBranch, StmtBuilder elseBranch) {
    StatementNode ifNode = IfNode.create(guard, thenBranch.build(), elseBranch.build());
    this.stmts.add(ifNode);
    return this;
  }

  /** create loops. */
  public StmtBuilder loop(StmtBuilder bodyBuilder) {
    StatementNode loop = LoopNode.create(bodyBuilder.build());
    this.stmts.add(loop);
    return this;
  }

  /** create break. */
  public StmtBuilder loopBreak() {
    StatementNode loopBreak = BreakNode.create(LiteralNode.create(IntegerValue.create(0)));
    this.stmts.add(loopBreak);
    return this;
  }

  /** build send stmt. */
  public StmtBuilder send(String recipient, ExpressionNode expr) {
    return send(ProcessName.create(recipient), expr);
  }

  /** build send stmt. */
  public StmtBuilder send(Host host, ExpressionNode expr) {
    this.stmts.add(SendNode.create(ProcessName.create(host), expr));
    return this;
  }

  /** build send stmt. */
  public StmtBuilder send(ProcessName recipient, ExpressionNode expr) {
    this.stmts.add(SendNode.create(recipient, expr));
    return this;
  }

  /** build recv stmt. */
  public StmtBuilder recv(String sender, String var) {
    return recv(ProcessName.create(sender), Variable.create(var));
  }

  /** build recv stmt. */
  public StmtBuilder recv(Host host, Variable var) {
    this.stmts.add(ReceiveNode.create(var, ProcessName.create(host)));
    return this;
  }

  /** build recv stmt. */
  public StmtBuilder recv(ProcessName sender, Variable var) {
    this.stmts.add(ReceiveNode.create(var, sender));
    return this;
  }

  /** build recv stmt. */
  public StmtBuilder recv(String sender, ImpType recvType, String var) {
    return recv(ProcessName.create(sender), recvType, Variable.create(var));
  }

  /** build recv stmt. */
  public StmtBuilder recv(ProcessName sender, ImpType recvType, Variable var) {
    this.stmts.add(ReceiveNode.create(var, recvType, sender));
    return this;
  }

  /** build assertion stmt. */
  public StmtBuilder assertion(ExpressionNode assertExpr) {
    StatementNode assertNode = AssertNode.create(assertExpr);
    this.stmts.add(assertNode);
    return this;
  }

  /** build generic stmt. */
  public StmtBuilder statement(StatementNode stmt) {
    this.stmts.add(stmt);
    return this;
  }

  /** concat two builders together. */
  public StmtBuilder concat(StmtBuilder other) {
    this.stmts.addAll(other.stmts);
    return this;
  }

  /** control context information. */
  private abstract static class ControlInfo {
    final List<StatementNode> prefix;
    final Map<ControlLabel, StatementNode> pathMap;
    ControlLabel currentPath;

    public ControlInfo(List<StatementNode> pref) {
      this.prefix = pref;
      this.pathMap = new HashMap<>();
      this.currentPath = null;
    }

    public void setCurrentPath(ControlLabel label) {
      this.currentPath = label;
    }

    public void finishCurrentPath(List<StatementNode> pathStmts) {
      assert this.currentPath != null;
      this.pathMap.put(this.currentPath, BlockNode.create(pathStmts));
      this.currentPath = null;
    }

    public List<StatementNode> pop() {
      StatementNode controlStructure = buildControlStructure();
      this.prefix.add(controlStructure);
      return this.prefix;
    }

    public abstract StatementNode buildControlStructure();
  }

  /** control context info about if statements. */
  private static class ConditionalControlInfo extends ControlInfo {
    final ExpressionNode guard;

    public ConditionalControlInfo(List<StatementNode> pref, ExpressionNode g) {
      super(pref);
      this.guard = g;
    }

    @Override
    public StatementNode buildControlStructure() {
      StatementNode thenBranch = this.pathMap.get(ControlLabel.THEN);
      StatementNode elseBranch = this.pathMap.get(ControlLabel.ELSE);
      return IfNode.create(this.guard, thenBranch, elseBranch);
    }
  }

  private static class LoopControlInfo extends ControlInfo {
    public LoopControlInfo(List<StatementNode> pref) {
      super(pref);
    }

    @Override
    public StatementNode buildControlStructure() {
      StatementNode body = this.pathMap.get(ControlLabel.BODY);
      return LoopNode.create(body);
    }
  }
}
