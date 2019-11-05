package edu.cornell.cs.apl.viaduct.imp.transformers;

import com.google.common.collect.ImmutableList;

import edu.cornell.cs.apl.viaduct.errors.ElaborationException;
import edu.cornell.cs.apl.viaduct.imp.ast.ArrayDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ArrayIndexingNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssertNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssignNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BinaryExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BreakNode;
import edu.cornell.cs.apl.viaduct.imp.ast.DowngradeNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ForNode;
import edu.cornell.cs.apl.viaduct.imp.ast.HostDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.IfNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LetBindingNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LiteralNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LoopNode;
import edu.cornell.cs.apl.viaduct.imp.ast.NotNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReadNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReceiveNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReferenceNode;
import edu.cornell.cs.apl.viaduct.imp.ast.SendNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StatementNode;
import edu.cornell.cs.apl.viaduct.imp.ast.TopLevelDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.imp.ast.VariableDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.WhileNode;
import edu.cornell.cs.apl.viaduct.imp.visitors.ExprVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.ProgramVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.ReferenceVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.TopLevelDeclarationVisitor;
import edu.cornell.cs.apl.viaduct.util.AbstractLineNumber;

/** inject logical position information to elaborated AST. */
public final class LogicalPositionInjector
    implements
        ProgramVisitor<ProgramNode>,
        TopLevelDeclarationVisitor<TopLevelDeclarationNode>,
        StmtVisitor<StatementNode>,
        ExprVisitor<ExpressionNode>,
        ReferenceVisitor<ReferenceNode>
{
  AbstractLineNumber nextPosition;

  public static ProgramNode run(ProgramNode node) {
    return node.accept(new LogicalPositionInjector());
  }

  public static StatementNode run(StatementNode node) {
    return node.accept(new LogicalPositionInjector("main"));
  }

  private LogicalPositionInjector() {
    this.nextPosition = new AbstractLineNumber("");
  }

  private LogicalPositionInjector(String main) {
    this.nextPosition = new AbstractLineNumber(main);
  }

  private LogicalPositionInjector(AbstractLineNumber cur, String main) {
    this.nextPosition = cur.addBranch(main);
  }

  private AbstractLineNumber getNextPosition() {
    AbstractLineNumber curPosition = this.nextPosition;
    this.nextPosition = curPosition.increment();
    return curPosition;
  }

  @Override
  public ProgramNode visit(ProgramNode node) {
    ImmutableList.Builder<TopLevelDeclarationNode> builder = ImmutableList.builder();
    for (TopLevelDeclarationNode decl : node.getDeclarations()) {
      builder.add(decl.accept(this));
    }

    return node.toBuilder().setDeclarations(builder.build()).build();
  }

  @Override
  public TopLevelDeclarationNode visit(HostDeclarationNode node) {
    return node;
  }

  @Override
  public TopLevelDeclarationNode visit(ProcessDeclarationNode node) {
    return
        node.toBuilder()
        .setBody(
            (BlockNode) node.getBody()
            .accept(new LogicalPositionInjector(node.getName().getName())))
        .build();
  }

  @Override
  public ReferenceNode visit(Variable var) {
    return
        var.toBuilder()
        .setLogicalPosition(getNextPosition())
        .build();
  }

  @Override
  public ReferenceNode visit(ArrayIndexingNode node) {
    return
        node.toBuilder()
        .setIndex(node.getIndex().accept(this))
        .setLogicalPosition(getNextPosition())
        .build();
  }

  @Override
  public ExpressionNode visit(LiteralNode node) {
    return
        node.toBuilder()
        .setLogicalPosition(getNextPosition())
        .build();
  }

  @Override
  public ExpressionNode visit(ReadNode node) {
    ReadNode newNode =
        node.toBuilder()
        .setReference(node.getReference().accept(this))
        .setLogicalPosition(getNextPosition())
        .build();

    return newNode;
  }

  @Override
  public ExpressionNode visit(NotNode node) {
    return
        node.toBuilder()
        .setExpression(node.getExpression().accept(this))
        .setLogicalPosition(getNextPosition())
        .build();
  }

  @Override
  public ExpressionNode visit(BinaryExpressionNode node) {
    return
        node.toBuilder()
        .setLhs(node.getLhs().accept(this))
        .setRhs(node.getRhs().accept(this))
        .setLogicalPosition(getNextPosition())
        .build();
  }

  @Override
  public ExpressionNode visit(DowngradeNode node) {
    return
        node.toBuilder()
        .setExpression(node.getExpression().accept(this))
        .setLogicalPosition(getNextPosition())
        .build();
  }

  @Override
  public StatementNode visit(VariableDeclarationNode node) {
    return
        node.toBuilder()
        .setLogicalPosition(getNextPosition())
        .build();
  }

  @Override
  public StatementNode visit(ArrayDeclarationNode node) {
    return
        node.toBuilder()
        .setLength(node.getLength().accept(this))
        .setLogicalPosition(getNextPosition())
        .build();
  }

  @Override
  public StatementNode visit(LetBindingNode node) {
    return
        node.toBuilder()
        .setRhs(node.getRhs().accept(this))
        .setLogicalPosition(getNextPosition())
        .build();
  }

  @Override
  public StatementNode visit(AssignNode node) {
    return
        node.toBuilder()
        .setLhs(node.getLhs().accept(this))
        .setRhs(node.getRhs().accept(this))
        .setLogicalPosition(getNextPosition())
        .build();
  }

  @Override
  public StatementNode visit(SendNode node) {
    return
        node.toBuilder()
        .setSentExpression(node.getSentExpression().accept(this))
        .setLogicalPosition(getNextPosition())
        .build();
  }

  @Override
  public StatementNode visit(ReceiveNode node) {
    return
        node.toBuilder()
        .setLogicalPosition(getNextPosition())
        .build();
  }

  @Override
  public StatementNode visit(IfNode node) {
    AbstractLineNumber curPosition = getNextPosition();
    return
        node.toBuilder()
        .setGuard(node.getGuard().accept(this))
        .setLogicalPosition(curPosition)
        .setThenBranch(
            (BlockNode) node.getThenBranch()
            .accept(new LogicalPositionInjector(curPosition, "then")))
        .setElseBranch(
            (BlockNode) node.getElseBranch()
            .accept(new LogicalPositionInjector(curPosition, "else")))
        .setLoopGuard(node.isLoopGuard())
        .build();
  }

  @Override
  public StatementNode visit(WhileNode node) {
    throw new ElaborationException();
  }

  @Override
  public StatementNode visit(ForNode forNode) {
    throw new ElaborationException();
  }

  @Override
  public StatementNode visit(LoopNode node) {
    AbstractLineNumber curPosition = getNextPosition();
    return
        node.toBuilder()
        .setLogicalPosition(curPosition)
        .setBody(
            (BlockNode) node.getBody()
            .accept(new LogicalPositionInjector(curPosition, "loop")))
        .build();
  }

  @Override
  public StatementNode visit(BreakNode node) {
    return
        node.toBuilder()
        .setLogicalPosition(getNextPosition())
        .build();
  }

  @Override
  public StatementNode visit(BlockNode node) {
    // AbstractLineNumber curPosition = getNextPosition();
    ImmutableList.Builder<StatementNode> builder = ImmutableList.builder();
    for (StatementNode stmt : node) {
      builder.add(stmt.accept(this));
    }
    return
        node.toBuilder()
        .setStatements(builder.build())
        .build();
  }

  @Override
  public StatementNode visit(AssertNode node) {
    return
        node.toBuilder()
        .setExpression(node.getExpression().accept(this))
        .setLogicalPosition(getNextPosition())
        .build();
  }
}
