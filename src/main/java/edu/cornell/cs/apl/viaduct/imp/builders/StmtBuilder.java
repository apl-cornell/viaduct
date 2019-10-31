package edu.cornell.cs.apl.viaduct.imp.builders;

import edu.cornell.cs.apl.viaduct.imp.ast.ArrayDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ArrayIndexingNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssertNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssignNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BreakNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.HostName;
import edu.cornell.cs.apl.viaduct.imp.ast.IfNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LetBindingNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LoopNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessName;
import edu.cornell.cs.apl.viaduct.imp.ast.ReceiveNode;
import edu.cornell.cs.apl.viaduct.imp.ast.SendNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StatementNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.imp.ast.VariableDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.types.ImpBaseType;
import edu.cornell.cs.apl.viaduct.pdg.ProgramDependencyGraph.ControlLabel;
import edu.cornell.cs.apl.viaduct.security.Label;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/** Builder for statements. Implicitly creates a sequence through a fluent interface. */
// TODO: remove once everthing is converted to builders.
public class StmtBuilder {
  private final Stack<ControlInfo> controlContext;
  private List<StatementNode> stmts;

  public StmtBuilder() {
    this.controlContext = new Stack<>();
    this.stmts = new ArrayList<>();
  }

  /** push a conditional into the builder control context. */
  public StmtBuilder pushIf(IfNode ifNode) {
    ControlInfo execPath = new ConditionalControlInfo(this.stmts, ifNode);
    this.controlContext.push(execPath);
    this.stmts = new ArrayList<>();
    return this;
  }

  /** push a loop into the builder control context. */
  public StmtBuilder pushLoop(LoopNode loopNode) {
    ControlInfo execPath = new LoopControlInfo(this.stmts, loopNode);
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
    return BlockNode.builder().setStatements(this.stmts).build();
  }

  /** Declare a new variable. */
  public StmtBuilder varDecl(Variable varName, ImpBaseType type, Label label) {
    this.stmts.add(
        VariableDeclarationNode.builder()
            .setVariable(varName)
            .setType(type)
            .setLabel(label)
            .build());
    return this;
  }

  /** Add array declaration node. */
  public StmtBuilder arrayDecl(
      Variable varName, ExpressionNode length, ImpBaseType type, Label label) {
    this.stmts.add(
        ArrayDeclarationNode.builder()
            .setVariable(varName)
            .setLength(length)
            .setElementType(type)
            .setLabel(label)
            .build());
    return this;
  }

  public StmtBuilder let(Variable varName, ExpressionNode rhs) {
    this.stmts.add(LetBindingNode.builder().setVariable(varName).setRhs(rhs).build());
    return this;
  }

  public StmtBuilder assign(Variable varName, ExpressionNode rhs) {
    this.stmts.add(AssignNode.builder().setLhs(varName).setRhs(rhs).build());
    return this;
  }

  /** Assign to an array index. */
  public StmtBuilder assign(Variable arrName, ExpressionNode idx, ExpressionNode rhs) {
    this.stmts.add(
        AssignNode.builder()
            .setLhs(ArrayIndexingNode.builder().setArray(arrName).setIndex(idx).build())
            .setRhs(rhs)
            .build());
    return this;
  }

  /** create break. */
  public StmtBuilder loopBreak() {
    this.stmts.add(BreakNode.builder().build());
    return this;
  }

  public StmtBuilder send(String recipient, ExpressionNode expr) {
    return send(ProcessName.create(recipient), expr);
  }

  public StmtBuilder send(HostName host, ExpressionNode expr) {
    return send(ProcessName.create(host), expr);
  }

  public StmtBuilder send(ProcessName recipient, ExpressionNode expr) {
    this.stmts.add(SendNode.builder().setRecipient(recipient).setSentExpression(expr).build());
    return this;
  }

  public StmtBuilder recv(HostName host, Variable var) {
    return recv(ProcessName.create(host), var);
  }

  public StmtBuilder recv(ProcessName sender, Variable var) {
    this.stmts.add(ReceiveNode.builder().setVariable(var).setSender(sender).build());
    return this;
  }

  /** build assertion stmt. */
  public StmtBuilder assertion(ExpressionNode assertExpr) {
    this.stmts.add(AssertNode.builder().setExpression(assertExpr).build());
    return this;
  }

  /** build generic stmt. */
  public StmtBuilder statement(StatementNode stmt) {
    this.stmts.add(stmt);
    return this;
  }

  /** control context information. */
  private abstract static class ControlInfo {
    final List<StatementNode> prefix;
    final Map<ControlLabel, BlockNode> pathMap;
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
      this.pathMap.put(this.currentPath, BlockNode.builder().setStatements(pathStmts).build());
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
    final IfNode ifNode;

    public ConditionalControlInfo(List<StatementNode> pref, IfNode ifNode) {
      super(pref);
      this.ifNode = ifNode;
    }

    @Override
    public StatementNode buildControlStructure() {
      BlockNode thenBranch = this.pathMap.get(ControlLabel.THEN);
      BlockNode elseBranch = this.pathMap.get(ControlLabel.ELSE);
      return IfNode.builder()
          .setGuard(this.ifNode.getGuard())
          .setThenBranch(thenBranch)
          .setElseBranch(elseBranch)
          .setSourceLocation(this.ifNode.getSourceLocation())
          .build();
    }
  }

  private static class LoopControlInfo extends ControlInfo {
    final LoopNode loopNode;

    public LoopControlInfo(List<StatementNode> pref, LoopNode loopNode) {
      super(pref);
      this.loopNode = loopNode;
    }

    @Override
    public StatementNode buildControlStructure() {
      BlockNode body = this.pathMap.get(ControlLabel.BODY);
      return LoopNode.builder()
          .setBody(body)
          .setSourceLocation(this.loopNode.getSourceLocation())
          .build();
    }
  }
}
