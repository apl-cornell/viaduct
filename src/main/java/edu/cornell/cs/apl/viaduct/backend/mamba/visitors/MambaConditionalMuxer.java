package edu.cornell.cs.apl.viaduct.backend.mamba.visitors;

import com.google.common.collect.ImmutableList;

import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaAssignNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaBinaryExpressionNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaBinaryOperators;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaBlockNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaExpressionNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaIfNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaInputNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaMuxNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaNegationNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaOutputNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaReadNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaRegIntDeclarationNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaSecurityType;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaStatementNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaVariable;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaWhileNode;
import edu.cornell.cs.apl.viaduct.util.FreshNameGenerator;

/** convert conditional statements into straightline code with muxes. */
public class MambaConditionalMuxer
    implements MambaStatementVisitor<Iterable<MambaStatementNode>>
{
  static final String GUARD_VAR_NAME = "guard";
  static final FreshNameGenerator nameGenerator = new FreshNameGenerator();

  MambaVariable guardVariable;

  public static Iterable<MambaStatementNode> run(MambaStatementNode stmt) {
    return stmt.accept(new MambaConditionalMuxer());
  }

  private MambaConditionalMuxer() {
    this.guardVariable = null;
  }

  private MambaConditionalMuxer(MambaVariable guardVariable) {
    this.guardVariable = guardVariable;
  }

  private static Iterable<MambaStatementNode> single(MambaStatementNode stmt) {
    return ImmutableList.of(stmt);
  }

  private static ImmutableList.Builder<MambaStatementNode> listBuilder() {
    return ImmutableList.builder();
  }

  @Override
  public Iterable<MambaStatementNode> visit(MambaRegIntDeclarationNode node) {
    return single(node);
  }

  @Override
  public Iterable<MambaStatementNode> visit(MambaAssignNode node) {
    MambaExpressionNode newRhs;

    if (this.guardVariable != null) {
      newRhs =
          MambaMuxNode.builder()
          .setGuard(MambaReadNode.create(this.guardVariable))
          .setThenValue(node.getRhs())
          .setElseValue(MambaReadNode.create(node.getVariable()))
          .build();

    } else {
      newRhs = node.getRhs();
    }

    return
      single(
          MambaAssignNode.builder()
          .setVariable(node.getVariable())
          .setRhs(newRhs)
          .build());
  }

  @Override
  public Iterable<MambaStatementNode> visit(MambaInputNode node) {
    if (this.guardVariable == null) {
      return single(node);

    } else {
      throw new Error("no I/O inside muxed conditionals");
    }
  }

  @Override
  public Iterable<MambaStatementNode> visit(MambaOutputNode node) {
    if (this.guardVariable == null) {
      return single(node);

    } else {
      throw new Error("no I/O inside muxed conditionals");
    }
  }

  @Override
  public Iterable<MambaStatementNode> visit(MambaIfNode node) {
    MambaExpressionNode guard = node.getGuard();
    MambaExpressionNode negatedGuard =
        MambaNegationNode.builder()
        .setExpression(guard)
        .build();

    MambaVariable condVar =
        MambaVariable.create(nameGenerator.getFreshName(GUARD_VAR_NAME));
    MambaVariable negCondVar =
        MambaVariable.create(nameGenerator.getFreshName(GUARD_VAR_NAME));

    MambaExpressionNode condAssign;
    MambaExpressionNode negCondAssign;
    if (this.guardVariable != null) {
      condAssign =
        MambaBinaryExpressionNode.builder()
        .setLhs(MambaReadNode.create(this.guardVariable))
        .setOperator(MambaBinaryOperators.And.create())
        .setRhs(guard)
        .build();

      negCondAssign =
        MambaBinaryExpressionNode.builder()
        .setLhs(MambaReadNode.create(this.guardVariable))
        .setOperator(MambaBinaryOperators.And.create())
        .setRhs(negatedGuard)
        .build();

    } else {
      condAssign = guard;
      negCondAssign = negatedGuard;
    }

    MambaBlockNode thenBranch = node.getThenBranch();
    MambaBlockNode elseBranch = node.getElseBranch();

    ImmutableList.Builder<MambaStatementNode> builder = listBuilder();

    if (thenBranch.getStatements().size() > 0) {
      builder
        .add(
            MambaRegIntDeclarationNode.builder()
            .setVariable(condVar)
            .setRegisterType(MambaSecurityType.SECRET)
            .build())
        .add(
            MambaAssignNode.builder()
            .setVariable(condVar)
            .setRhs(condAssign)
            .build())
        .addAll(node.getThenBranch().accept(new MambaConditionalMuxer(condVar)));
    }

    if (elseBranch.getStatements().size() > 0) {
      builder
        .add(
            MambaRegIntDeclarationNode.builder()
            .setVariable(negCondVar)
            .setRegisterType(MambaSecurityType.SECRET)
            .build())
          .add(
              MambaAssignNode.builder()
              .setVariable(negCondVar)
              .setRhs(negCondAssign)
              .build())
          .addAll(node.getElseBranch().accept(new MambaConditionalMuxer(negCondVar)));
    }

    return builder.build();
  }

  @Override
  public Iterable<MambaStatementNode> visit(MambaWhileNode node) {
    return single(
        node.toBuilder()
        .setBody(MambaBlockNode.create(node.getBody().accept(this)))
        .build());
  }

  @Override
  public Iterable<MambaStatementNode> visit(MambaBlockNode node) {
    ImmutableList.Builder<MambaStatementNode> builder = listBuilder();
    for (MambaStatementNode stmt : node) {
      builder.addAll(stmt.accept(this));
    }
    return builder.build();
  }
}
