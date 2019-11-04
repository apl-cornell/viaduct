package edu.cornell.cs.apl.viaduct.backend.mamba.visitors;

import com.google.common.collect.ImmutableList;

import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaAssignNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaBinaryExpressionNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaBinaryOperator;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaBinaryOperators;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaBlockNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaExpressionNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaIfNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaInputNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaIntLiteralNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaOutputNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaReadNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaRegIntDeclarationNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaSecurityType;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaStatementNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaVariable;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaWhileNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ArrayDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ArrayIndexingNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssertNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssignNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BinaryExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BinaryOperator;
import edu.cornell.cs.apl.viaduct.imp.ast.BinaryOperators;
import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BreakNode;
import edu.cornell.cs.apl.viaduct.imp.ast.DowngradeNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ForNode;
import edu.cornell.cs.apl.viaduct.imp.ast.IfNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LetBindingNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LiteralNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LoopNode;
import edu.cornell.cs.apl.viaduct.imp.ast.NotNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReadNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReceiveNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReferenceNode;
import edu.cornell.cs.apl.viaduct.imp.ast.SendNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StatementNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.imp.ast.VariableDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.WhileNode;
import edu.cornell.cs.apl.viaduct.imp.ast.values.BooleanValue;
import edu.cornell.cs.apl.viaduct.imp.ast.values.ImpValue;
import edu.cornell.cs.apl.viaduct.imp.ast.values.IntegerValue;
import edu.cornell.cs.apl.viaduct.imp.visitors.ExprVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.ReferenceVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;
import edu.cornell.cs.apl.viaduct.util.FreshNameGenerator;

public final class ImpToMambaTranslator
    implements
        ExprVisitor<MambaExpressionNode>,
        StmtVisitor<Iterable<MambaStatementNode>>,
        ReferenceVisitor<MambaVariable>
{
  private static FreshNameGenerator nameGenerator = new FreshNameGenerator();
  private static String LOOP_VAR = "loop_cond";

  private final boolean isSecret;
  private final MambaVariable currentLoopVar;

  public static MambaStatementNode run(boolean isSecret, StatementNode stmt) {
    return MambaBlockNode.create(stmt.accept(new ImpToMambaTranslator(isSecret)));
  }

  /** run under context where loopVar is the guard of the current loop. */
  public static MambaStatementNode run(
      boolean isSecret,
      MambaVariable loopVar,
      StatementNode stmt) {

    ImmutableList<MambaStatementNode> mambaStmts =
        ImmutableList.copyOf(stmt.accept(new ImpToMambaTranslator(isSecret, loopVar)));

    if (mambaStmts.size() == 1) {
      return mambaStmts.get(0);

    } else {
      return MambaBlockNode.create(mambaStmts);
    }
  }

  public static MambaExpressionNode run(boolean isSecret, ExpressionNode expr) {
    return expr.accept(new ImpToMambaTranslator(isSecret));
  }

  public static MambaVariable run(boolean isSecret, ReferenceNode ref) {
    return ref.accept(new ImpToMambaTranslator(isSecret));
  }

  public static MambaVariable getFreshLoopVariable() {
    return MambaVariable.create(nameGenerator.getFreshName(LOOP_VAR));
  }

  private static Iterable<MambaStatementNode> single(MambaStatementNode stmt) {
    return ImmutableList.of(stmt);
  }

  private static ImmutableList.Builder<MambaStatementNode> listBuilder() {
    return ImmutableList.builder();
  }

  private ImpToMambaTranslator(boolean isSecret) {
    this.isSecret = isSecret;
    this.currentLoopVar = null;
  }

  private ImpToMambaTranslator(boolean isSecret, MambaVariable loopVar) {
    this.isSecret = isSecret;
    this.currentLoopVar = loopVar;
  }

  private MambaSecurityType getSecurityContext() {
    return this.isSecret ? MambaSecurityType.SECRET : MambaSecurityType.CLEAR;
  }

  @Override
  public MambaVariable visit(Variable var) {
    return MambaVariable.create(var.getName());
  }

  @Override
  public MambaVariable visit(ArrayIndexingNode arrayIndex) {
    throw new Error("translation not implemented");
  }

  @Override
  public MambaExpressionNode visit(LiteralNode node) {
    ImpValue value = node.getValue();

    // mamba has no native bool type, so convert to int
    int n;
    if (value instanceof BooleanValue) {
      BooleanValue boolValue = (BooleanValue) value;
      n = boolValue.getValue() ? 1 : 0;

    } else {
      IntegerValue intValue = (IntegerValue) value;
      n = intValue.getValue();
    }

    return
        MambaIntLiteralNode.builder()
        .setSecurityType(getSecurityContext())
        .setValue(n)
        .build();
  }

  @Override
  public MambaExpressionNode visit(ReadNode node) {
    return MambaReadNode.create(node.getReference().accept(this));
  }

  @Override
  public MambaExpressionNode visit(NotNode node) {
    throw new Error("translation not implemented");
  }

  @Override
  public MambaExpressionNode visit(BinaryExpressionNode node) {
    MambaExpressionNode mambaLhs = node.getLhs().accept(this);
    MambaExpressionNode mambaRhs = node.getRhs().accept(this);

    BinaryOperator binOp = node.getOperator();
    MambaBinaryOperator mambaBinOp;
    if (binOp instanceof BinaryOperators.Or) {
      mambaBinOp = MambaBinaryOperators.Or.create();

    } else if (binOp instanceof BinaryOperators.And) {
      mambaBinOp = MambaBinaryOperators.And.create();

    } else if (binOp instanceof BinaryOperators.EqualTo) {
      mambaBinOp = MambaBinaryOperators.EqualTo.create();

    } else if (binOp instanceof BinaryOperators.LessThan) {
      mambaBinOp = MambaBinaryOperators.LessThan.create();

    } else if (binOp instanceof BinaryOperators.LessThanOrEqualTo) {
      mambaBinOp = MambaBinaryOperators.LessThanOrEqualTo.create();

    } else if (binOp instanceof BinaryOperators.Plus) {
      mambaBinOp = MambaBinaryOperators.Plus.create();

    } else if (binOp instanceof BinaryOperators.Minus) {
      mambaBinOp = MambaBinaryOperators.Minus.create();

    } else if (binOp instanceof BinaryOperators.Times) {
      mambaBinOp = MambaBinaryOperators.Times.create();

    } else {
      throw new Error("translation of binary operator not implemented");
    }

    return
        MambaBinaryExpressionNode.builder()
        .setLhs(mambaLhs)
        .setRhs(mambaRhs)
        .setOperator(mambaBinOp)
        .build();
  }

  @Override
  public MambaExpressionNode visit(DowngradeNode node) {
    return node.getExpression().accept(this);
  }

  @Override
  public Iterable<MambaStatementNode> visit(VariableDeclarationNode node) {
    return
        single(
            MambaRegIntDeclarationNode.builder()
            .setRegisterType(getSecurityContext())
            .setVariable(node.getVariable().accept(this))
            .build());
  }

  @Override
  public Iterable<MambaStatementNode> visit(ArrayDeclarationNode node) {
    throw new Error("translation not implemented");
  }

  @Override
  public Iterable<MambaStatementNode> visit(LetBindingNode node) {
    MambaExpressionNode mambaRhs = node.getRhs().accept(this);
    return
        single(
            MambaAssignNode.builder()
            .setVariable(node.getVariable().accept(this))
            .setRhs(mambaRhs)
            .build());
  }

  @Override
  public Iterable<MambaStatementNode> visit(AssignNode node) {
    MambaVariable mambaVar = node.getLhs().accept(this);
    MambaExpressionNode mambaRhs = node.getRhs().accept(this);
    return
        single(
            MambaAssignNode.builder()
            .setVariable(mambaVar)
            .setRhs(mambaRhs)
            .build());
  }

  @Override
  public Iterable<MambaStatementNode> visit(SendNode node) {
    return
        single(
            MambaOutputNode.builder()
            .setExpression(node.getSentExpression().accept(this))
            .setPlayer(0) // TODO: fix this
            .build());
  }

  @Override
  public Iterable<MambaStatementNode> visit(ReceiveNode node) {
    return
        single(
            MambaInputNode.builder()
            .setVariable(node.getVariable().accept(this))
            .setPlayer(0) // TODO: fix this
            .setSecurityContext(getSecurityContext())
            .build());
  }

  @Override
  public Iterable<MambaStatementNode> visit(IfNode node) {
    return
        single(
            MambaIfNode.builder()
            .setGuard(node.getGuard().accept(this))
            .setThenBranch(MambaBlockNode.create(node.getThenBranch().accept(this)))
            .setElseBranch(MambaBlockNode.create(node.getElseBranch().accept(this)))
            .build());
  }

  @Override
  public Iterable<MambaStatementNode> visit(WhileNode node) {
    return
        single(
            MambaWhileNode.builder()
            .setGuard(node.getGuard().accept(this))
            .setBody(MambaBlockNode.create(node.getBody().accept(this)))
            .build());
  }

  @Override
  public Iterable<MambaStatementNode> visit(ForNode node) {
    throw new Error("translation not implemented");
  }

  @Override
  public Iterable<MambaStatementNode> visit(LoopNode node) {
    // convert to while loop by generating a new loop variable
    MambaVariable loopVar = getFreshLoopVariable();
    Iterable<MambaStatementNode> body =
        node.getBody().accept(new ImpToMambaTranslator(isSecret, loopVar));

    return
        listBuilder()
        .add(
            MambaRegIntDeclarationNode.builder()
            .setRegisterType(getSecurityContext())
            .setVariable(loopVar)
            .build())
        .add(
            MambaAssignNode.builder()
            .setVariable(loopVar)
            .setRhs(
                MambaIntLiteralNode.builder()
                .setSecurityType(getSecurityContext())
                .setValue(1)
                .build())
            .build())
        .add(
            MambaWhileNode.builder()
            .setGuard(MambaReadNode.create(loopVar))
            .setBody(MambaBlockNode.create(body))
            .build())
        .build();
  }

  @Override
  public Iterable<MambaStatementNode> visit(BreakNode node) {
    if (this.currentLoopVar != null) {
      return
          single(
              MambaAssignNode.builder()
              .setVariable(this.currentLoopVar)
              .setRhs(
                  MambaIntLiteralNode.builder()
                  .setSecurityType(getSecurityContext())
                  .setValue(0)
                  .build())
              .build());

    } else {
      throw new Error("cannot break outside of a loop");
    }
  }

  @Override
  public Iterable<MambaStatementNode> visit(BlockNode node) {
    ImmutableList.Builder<MambaStatementNode> builder = ImmutableList.builder();
    for (StatementNode stmt : node) {
      builder.addAll(stmt.accept(this));
    }
    return builder.build();
  }

  @Override
  public Iterable<MambaStatementNode> visit(AssertNode node) {
    throw new Error("translation not implemented");
  }
}
