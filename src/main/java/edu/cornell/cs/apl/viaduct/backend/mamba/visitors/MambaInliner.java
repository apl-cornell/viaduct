package edu.cornell.cs.apl.viaduct.backend.mamba.visitors;

import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaAssignNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaBinaryExpressionNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaBlockNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaExpressionNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaIfNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaInputNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaIntLiteralNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaOutputNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaReadNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaRegIntDeclarationNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaRevealNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaStatementNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaVariable;

import io.vavr.collection.Map;

import java.util.ArrayList;
import java.util.List;

/** inline expressions by substituting variables. */
public class MambaInliner
    implements
        MambaExpressionVisitor<MambaExpressionNode>,
        MambaStatementVisitor<MambaStatementNode>
{
  private final Map<MambaVariable, MambaExpressionNode> inlineMap;

  private MambaInliner(Map<MambaVariable, MambaExpressionNode> inlineMap) {
    this.inlineMap = inlineMap;
  }

  public static MambaExpressionNode run(
      Map<MambaVariable, MambaExpressionNode> inlineMap,
      MambaExpressionNode expr)
  {
    return expr.accept(new MambaInliner(inlineMap));
  }

  public static MambaStatementNode run(
      Map<MambaVariable, MambaExpressionNode> inlineMap,
      MambaStatementNode stmt)
  {
    return stmt.accept(new MambaInliner(inlineMap));
  }

  @Override
  public MambaExpressionNode visit(MambaIntLiteralNode node) {
    return node;
  }

  @Override
  public MambaExpressionNode visit(MambaReadNode node) {
    MambaVariable var = node.getVariable();
    if (this.inlineMap.containsKey(var)) {
      return this.inlineMap.getOrElse(var, null);

    } else {
      return node;
    }
  }

  @Override
  public MambaExpressionNode visit(MambaBinaryExpressionNode node) {
    return
      node.toBuilder()
      .setLhs(node.getLhs().accept(this))
      .setRhs(node.getRhs().accept(this))
      .build();
  }

  @Override
  public MambaExpressionNode visit(MambaRevealNode node) {
    return
      node.toBuilder()
      .setRevealedExpr(node.getRevealedExpr().accept(this))
      .build();
  }

  @Override
  public MambaStatementNode visit(MambaRegIntDeclarationNode node) {
    return node;
  }

  @Override
  public MambaStatementNode visit(MambaAssignNode node) {
    return
        node.toBuilder()
        .setRhs(node.getRhs().accept(this))
        .build();
  }

  @Override
  public MambaStatementNode visit(MambaInputNode node) {
    return node;
  }

  @Override
  public MambaStatementNode visit(MambaOutputNode node) {
    return
      node.toBuilder()
      .setExpression(node.getExpression().accept(this))
      .build();
  }

  @Override
  public MambaStatementNode visit(MambaIfNode node) {
    return
        node.toBuilder()
        .setGuard(node.getGuard().accept(this))
        .setThenBranch((MambaBlockNode) node.getThenBranch().accept(this))
        .setElseBranch((MambaBlockNode) node.getElseBranch().accept(this))
        .build();
  }

  @Override
  public MambaStatementNode visit(MambaBlockNode node) {
    List<MambaStatementNode> newStmts = new ArrayList<>();
    for (MambaStatementNode stmt : node) {
      newStmts.add(stmt.accept(this));
    }

    return MambaBlockNode.builder().addAll(newStmts).build();
  }
}
