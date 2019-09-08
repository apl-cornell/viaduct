package edu.cornell.cs.apl.viaduct.imp.visitors;

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
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
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

/**
 * A visitor that traverses the AST and returns it unchanged.
 *
 * <p>This class provides a default "change nothing" behavior, and is meant as a template for other
 * visitors. Visitors that only change a small subset of AST nodes should inherit from this class
 * and override only the cases that do something interesting.
 */
public abstract class IdentityProgramVisitor
    extends AbstractProgramVisitor<IdentityProgramVisitor, TopLevelDeclarationNode, ProgramNode> {
  private final RunVisitor runner = new RunVisitor();

  private final ReferenceVisitor<ReferenceNode> referenceVisitor = new IdentityReferenceVisitor();
  private final ExprVisitor<ExpressionNode> expressionVisitor = new IdentityExprVisitor();
  private final StmtVisitor<StatementNode> statementVisitor = new IdentityStmtVisitor();
  private final TopLevelDeclarationVisitor<TopLevelDeclarationNode> declarationVisitor =
      new IdentityDeclarationVisitor();

  public final ImpAstNode run(ImpAstNode node) {
    return node.accept(runner);
  }

  public final ReferenceNode run(ReferenceNode reference) {
    return runner.visit(reference);
  }

  public final ExpressionNode run(ExpressionNode expression) {
    return runner.visit(expression);
  }

  public final StatementNode run(StatementNode statement) {
    return runner.visit(statement);
  }

  public final ProgramNode run(ProgramNode program) {
    return runner.visit(program);
  }

  protected ReferenceVisitor<ReferenceNode> getReferenceVisitor() {
    return referenceVisitor;
  }

  protected ExprVisitor<ExpressionNode> getExpressionVisitor() {
    return expressionVisitor;
  }

  protected StmtVisitor<StatementNode> getStatementVisitor() {
    return statementVisitor;
  }

  @Override
  protected TopLevelDeclarationVisitor<TopLevelDeclarationNode> getDeclarationVisitor() {
    return declarationVisitor;
  }

  @Override
  protected IdentityProgramVisitor enter(ProgramNode node) {
    return this;
  }

  @Override
  protected ProgramNode leave(
      ProgramNode node,
      IdentityProgramVisitor visitor,
      Iterable<TopLevelDeclarationNode> declarations) {
    return node.toBuilder().setDeclarations(declarations).build();
  }

  protected class IdentityDeclarationVisitor
      extends AbstractTopLevelDeclarationVisitor<
          IdentityDeclarationVisitor, StatementNode, TopLevelDeclarationNode> {

    @Override
    protected final StmtVisitor<StatementNode> getStatementVisitor() {
      return IdentityProgramVisitor.this.getStatementVisitor();
    }

    @Override
    protected IdentityDeclarationVisitor enter(TopLevelDeclarationNode node) {
      return this;
    }

    @Override
    protected TopLevelDeclarationNode leave(
        ProcessDeclarationNode node, IdentityDeclarationVisitor visitor, StatementNode body) {
      return node.toBuilder().setBody((BlockNode) body).build();
    }

    @Override
    protected TopLevelDeclarationNode leave(
        HostDeclarationNode node, IdentityDeclarationVisitor visitor) {
      return node;
    }
  }

  protected class IdentityStmtVisitor
      extends AbstractStmtVisitor<
          IdentityStmtVisitor, ReferenceNode, ExpressionNode, StatementNode> {

    @Override
    protected final ReferenceVisitor<ReferenceNode> getReferenceVisitor() {
      return IdentityProgramVisitor.this.getReferenceVisitor();
    }

    @Override
    protected final ExprVisitor<ExpressionNode> getExpressionVisitor() {
      return IdentityProgramVisitor.this.getExpressionVisitor();
    }

    @Override
    protected IdentityStmtVisitor enter(StatementNode node) {
      return this;
    }

    @Override
    protected StatementNode leave(VariableDeclarationNode node, IdentityStmtVisitor visitor) {
      return node;
    }

    @Override
    protected StatementNode leave(
        ArrayDeclarationNode node, IdentityStmtVisitor visitor, ExpressionNode length) {
      return node.toBuilder().setLength(length).build();
    }

    @Override
    protected StatementNode leave(
        LetBindingNode node, IdentityStmtVisitor visitor, ExpressionNode rhs) {
      return node.toBuilder().setRhs(rhs).build();
    }

    @Override
    protected StatementNode leave(
        AssignNode node, IdentityStmtVisitor visitor, ReferenceNode lhs, ExpressionNode rhs) {
      return node.toBuilder().setLhs(lhs).setRhs(rhs).build();
    }

    @Override
    protected StatementNode leave(
        SendNode node, IdentityStmtVisitor visitor, ExpressionNode sentExpression) {
      return node.toBuilder().setSentExpression(sentExpression).build();
    }

    @Override
    protected StatementNode leave(
        ReceiveNode node, IdentityStmtVisitor visitor, ReferenceNode lhs) {
      return node.toBuilder().setVariable((Variable) lhs).build();
    }

    @Override
    protected StatementNode leave(
        IfNode node,
        IdentityStmtVisitor visitor,
        ExpressionNode guard,
        StatementNode thenBranch,
        StatementNode elseBranch) {
      return node.toBuilder()
          .setGuard(guard)
          .setThenBranch((BlockNode) thenBranch)
          .setElseBranch((BlockNode) elseBranch)
          .build();
    }

    @Override
    protected StatementNode leave(
        WhileNode node, IdentityStmtVisitor visitor, ExpressionNode guard, StatementNode body) {
      return node.toBuilder().setGuard(guard).setBody((BlockNode) body).build();
    }

    @Override
    protected StatementNode leave(
        ForNode node,
        IdentityStmtVisitor visitor,
        StatementNode initialize,
        ExpressionNode guard,
        StatementNode update,
        StatementNode body) {
      return node.toBuilder()
          .setInitialize(initialize)
          .setGuard(guard)
          .setUpdate(update)
          .setBody((BlockNode) body)
          .build();
    }

    @Override
    protected StatementNode leave(LoopNode node, IdentityStmtVisitor visitor, StatementNode body) {
      return node.toBuilder().setBody((BlockNode) body).build();
    }

    @Override
    protected StatementNode leave(BreakNode node, IdentityStmtVisitor visitor) {
      return node;
    }

    @Override
    protected StatementNode leave(
        BlockNode node, IdentityStmtVisitor visitor, Iterable<StatementNode> statements) {
      return node.toBuilder().setStatements(statements).build();
    }

    @Override
    protected StatementNode leave(
        AssertNode node, IdentityStmtVisitor visitor, ExpressionNode expression) {
      return node.toBuilder().setExpression(expression).build();
    }
  }

  protected class IdentityExprVisitor
      extends AbstractExprVisitor<IdentityExprVisitor, ReferenceNode, ExpressionNode> {

    @Override
    protected final ReferenceVisitor<ReferenceNode> getReferenceVisitor() {
      return IdentityProgramVisitor.this.getReferenceVisitor();
    }

    @Override
    protected IdentityExprVisitor enter(ExpressionNode node) {
      return this;
    }

    @Override
    protected ExpressionNode leave(LiteralNode node, IdentityExprVisitor visitor) {
      return node;
    }

    @Override
    protected ExpressionNode leave(
        ReadNode node, IdentityExprVisitor visitor, ReferenceNode reference) {
      return node.toBuilder().setReference(reference).build();
    }

    @Override
    protected ExpressionNode leave(
        NotNode node, IdentityExprVisitor visitor, ExpressionNode expression) {
      return node.toBuilder().setExpression(expression).build();
    }

    @Override
    protected ExpressionNode leave(
        BinaryExpressionNode node,
        IdentityExprVisitor visitor,
        ExpressionNode lhs,
        ExpressionNode rhs) {
      return node.toBuilder().setLhs(lhs).setRhs(rhs).build();
    }

    @Override
    protected ExpressionNode leave(
        DowngradeNode node, IdentityExprVisitor visitor, ExpressionNode expression) {
      return node.toBuilder().setExpression(expression).build();
    }
  }

  protected class IdentityReferenceVisitor
      extends AbstractReferenceVisitor<IdentityReferenceVisitor, ReferenceNode, ExpressionNode> {

    @Override
    protected final ExprVisitor<ExpressionNode> getExpressionVisitor() {
      return IdentityProgramVisitor.this.getExpressionVisitor();
    }

    @Override
    protected IdentityReferenceVisitor enter(ReferenceNode node) {
      return this;
    }

    @Override
    protected ReferenceNode leave(Variable node, IdentityReferenceVisitor visitor) {
      return node;
    }

    @Override
    protected ReferenceNode leave(
        ArrayIndexingNode node,
        IdentityReferenceVisitor visitor,
        ReferenceNode array,
        ExpressionNode index) {
      return node.toBuilder().setArray((Variable) array).setIndex(index).build();
    }
  }

  private class RunVisitor implements ImpAstVisitor<ImpAstNode> {
    @Override
    public ReferenceNode visit(ReferenceNode node) {
      return node.accept(getReferenceVisitor());
    }

    @Override
    public ExpressionNode visit(ExpressionNode node) {
      return node.accept(getExpressionVisitor());
    }

    @Override
    public StatementNode visit(StatementNode node) {
      return node.accept(getStatementVisitor());
    }

    @Override
    public TopLevelDeclarationNode visit(TopLevelDeclarationNode node) {
      return node.accept(getDeclarationVisitor());
    }

    @Override
    public ProgramNode visit(ProgramNode node) {
      return node.accept(IdentityProgramVisitor.this);
    }
  }
}
