package edu.cornell.cs.apl.viaduct.backend.mamba.visitors;

import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaArrayDeclarationNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaArrayLoadNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaArrayStoreNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaAssignNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaBinaryExpressionNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaBlockNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaExpressionNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaIfNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaInputNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaIntLiteralNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaMuxNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaNegationNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaOutputNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaReadNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaRegIntDeclarationNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaRevealNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaStatementNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaWhileNode;
import java.util.ArrayList;
import java.util.List;

/** inline expressions by substituting variables. */
public class MambaIdentityVisitor
    implements MambaExpressionVisitor<MambaExpressionNode>,
        MambaStatementVisitor<MambaStatementNode> {
  @Override
  public MambaExpressionNode visit(MambaIntLiteralNode node) {
    return node;
  }

  @Override
  public MambaExpressionNode visit(MambaReadNode node) {
    return node;
  }

  @Override
  public MambaExpressionNode visit(MambaArrayLoadNode node) {
    return node.toBuilder().setIndex(node.getIndex().accept(this)).build();
  }

  @Override
  public MambaExpressionNode visit(MambaBinaryExpressionNode node) {
    return node.toBuilder()
        .setLhs(node.getLhs().accept(this))
        .setRhs(node.getRhs().accept(this))
        .build();
  }

  @Override
  public MambaExpressionNode visit(MambaNegationNode node) {
    return node.toBuilder().setExpression(node.getExpression().accept(this)).build();
  }

  @Override
  public MambaExpressionNode visit(MambaRevealNode node) {
    return node.toBuilder().setRevealedExpr(node.getRevealedExpr().accept(this)).build();
  }

  @Override
  public MambaExpressionNode visit(MambaMuxNode node) {
    return node.toBuilder()
        .setGuard(node.getGuard().accept(this))
        .setThenValue(node.getGuard().accept(this))
        .setElseValue(node.getGuard().accept(this))
        .build();
  }

  @Override
  public MambaStatementNode visit(MambaRegIntDeclarationNode node) {
    return node;
  }

  @Override
  public MambaStatementNode visit(MambaArrayDeclarationNode node) {
    return node.toBuilder().setLength(node.getLength().accept(this)).build();
  }

  @Override
  public MambaStatementNode visit(MambaAssignNode node) {
    return node.toBuilder().setRhs(node.getRhs().accept(this)).build();
  }

  @Override
  public MambaStatementNode visit(MambaArrayStoreNode node) {
    return node.toBuilder()
        .setIndex(node.getIndex().accept(this))
        .setValue(node.getValue().accept(this))
        .build();
  }

  @Override
  public MambaStatementNode visit(MambaInputNode node) {
    return node;
  }

  @Override
  public MambaStatementNode visit(MambaOutputNode node) {
    return node.toBuilder().setExpression(node.getExpression().accept(this)).build();
  }

  @Override
  public MambaStatementNode visit(MambaIfNode node) {
    return node.toBuilder()
        .setGuard(node.getGuard().accept(this))
        .setThenBranch((MambaBlockNode) node.getThenBranch().accept(this))
        .setElseBranch((MambaBlockNode) node.getElseBranch().accept(this))
        .build();
  }

  @Override
  public MambaStatementNode visit(MambaWhileNode node) {
    return node.toBuilder()
        .setGuard(node.getGuard().accept(this))
        .setBody((MambaBlockNode) node.getBody().accept(this))
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
