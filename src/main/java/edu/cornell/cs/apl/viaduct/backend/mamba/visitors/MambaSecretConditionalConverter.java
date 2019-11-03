package edu.cornell.cs.apl.viaduct.backend.mamba.visitors;

import com.google.common.collect.ImmutableList;

import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaAssignNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaBinaryExpressionNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaBlockNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaIfNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaInputNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaIntLiteralNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaMuxNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaNegationNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaOutputNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaReadNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaRegIntDeclarationNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaRevealNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaSecurityType;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaStatementNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaVariable;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaWhileNode;
import io.vavr.collection.HashSet;
import io.vavr.collection.Set;

/** convert conditionals with secret guard into straightline code by muxing them. */
public class MambaSecretConditionalConverter
    implements MambaStatementVisitor<Iterable<MambaStatementNode>>
{
  protected Set<MambaVariable> secretVariables;

  /** run visitor. */
  public static MambaStatementNode run(MambaStatementNode stmt) {
    return
        MambaBlockNode.builder()
        .addAll(stmt.accept(new MambaSecretConditionalConverter()))
        .build();
  }

  private static Iterable<MambaStatementNode> single(MambaStatementNode stmt) {
    return ImmutableList.of(stmt);
  }

  private static ImmutableList.Builder<MambaStatementNode> listBuilder() {
    return ImmutableList.builder();
  }

  private MambaSecretConditionalConverter() {
    this.secretVariables = HashSet.empty();
  }

  private MambaSecretConditionalConverter(Set<MambaVariable> secretVariables) {
    this.secretVariables = secretVariables;
  }

  private void addSecretVariable(MambaVariable var) {
    this.secretVariables = this.secretVariables.add(var);
  }

  @Override
  public Iterable<MambaStatementNode> visit(MambaRegIntDeclarationNode node) {
    if (node.getRegisterType() == MambaSecurityType.SECRET) {
      addSecretVariable(node.getVariable());
    }
    return single(node);
  }

  @Override
  public Iterable<MambaStatementNode> visit(MambaAssignNode node) {
    if (node.getRhs().accept(new SecretInputChecker())) {
      addSecretVariable(node.getVariable());
    }
    return single(node);
  }

  @Override
  public Iterable<MambaStatementNode> visit(MambaInputNode node) {
    if (node.getPlayer() >= 0) {
      addSecretVariable(node.getVariable());
    }
    return single(node);
  }

  @Override
  public Iterable<MambaStatementNode> visit(MambaOutputNode node) {
    return single(node);
  }

  @Override
  public Iterable<MambaStatementNode> visit(MambaIfNode node) {
    /** conditional is secret! mux conditional */
    if (node.getGuard().accept(new SecretInputChecker())) {
      return MambaConditionalMuxer.run(node);

    } else {
      MambaSecretConditionalConverter newConverter =
          new MambaSecretConditionalConverter(this.secretVariables);
      return
          single(
              MambaIfNode.builder()
              .setGuard(node.getGuard())
              .setThenBranch(
                  MambaBlockNode.create(node.getThenBranch().accept(newConverter)))
              .setElseBranch(
                  MambaBlockNode.create(node.getElseBranch().accept(newConverter)))
              .build());
    }
  }

  @Override
  public Iterable<MambaStatementNode> visit(MambaWhileNode node) {
    return
        single(
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

  /** check if secret data is used in an expression. */
  private class SecretInputChecker implements MambaExpressionVisitor<Boolean> {
    @Override
    public Boolean visit(MambaIntLiteralNode node) {
      return false;
    }

    @Override
    public Boolean visit(MambaReadNode node) {
      return
          MambaSecretConditionalConverter.this.secretVariables
          .contains(node.getVariable());
    }

    @Override
    public Boolean visit(MambaBinaryExpressionNode node) {
      return node.getLhs().accept(this) || node.getRhs().accept(this);
    }

    @Override
    public Boolean visit(MambaNegationNode node) {
      return node.getExpression().accept(this);
    }

    @Override
    public Boolean visit(MambaRevealNode node) {
      // don't count inputs of reveal node as secret,
      // since they were explicitly declassified
      return false;
    }

    @Override
    public Boolean visit(MambaMuxNode node) {
      return
          node.getGuard().accept(this)
          || node.getThenValue().accept(this)
          || node.getElseValue().accept(this);
    }
  }
}
