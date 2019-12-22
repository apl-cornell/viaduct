package edu.cornell.cs.apl.viaduct.backend.mamba.visitors;

import com.google.common.collect.ImmutableList;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaArrayDeclarationNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaArrayStoreNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaAssignNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaBlockNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaIfNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaInputNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaOutputNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaRegIntDeclarationNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaStatementNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaWhileNode;

/** convert conditionals with secret guard into straightline code by muxing them. */
public class MambaSecretConditionalConverter
    implements MambaStatementVisitor<Iterable<MambaStatementNode>> {
  private final MambaSecretInputChecker secretInputChecker;

  /** run visitor. */
  public static MambaStatementNode run(
      MambaSecretInputChecker secretInputChecker, MambaStatementNode stmt) {
    return MambaBlockNode.builder()
        .addAll(stmt.accept(new MambaSecretConditionalConverter(secretInputChecker)))
        .build();
  }

  private MambaSecretConditionalConverter(MambaSecretInputChecker secretInputChecker) {
    this.secretInputChecker = secretInputChecker;
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
  public Iterable<MambaStatementNode> visit(MambaArrayDeclarationNode node) {
    return single(node);
  }

  @Override
  public Iterable<MambaStatementNode> visit(MambaAssignNode node) {
    return single(node);
  }

  @Override
  public Iterable<MambaStatementNode> visit(MambaArrayStoreNode node) {
    return single(node);
  }

  @Override
  public Iterable<MambaStatementNode> visit(MambaInputNode node) {
    return single(node);
  }

  @Override
  public Iterable<MambaStatementNode> visit(MambaOutputNode node) {
    return single(node);
  }

  @Override
  public Iterable<MambaStatementNode> visit(MambaIfNode node) {
    /** conditional is secret! mux conditional */
    if (node.getGuard().accept(this.secretInputChecker)) {
      return MambaConditionalMuxer.run(node);

    } else {
      return single(
          MambaIfNode.builder()
              .setGuard(node.getGuard())
              .setThenBranch(MambaBlockNode.create(node.getThenBranch().accept(this)))
              .setElseBranch(MambaBlockNode.create(node.getElseBranch().accept(this)))
              .build());
    }
  }

  @Override
  public Iterable<MambaStatementNode> visit(MambaWhileNode node) {
    return single(
        node.toBuilder().setBody(MambaBlockNode.create(node.getBody().accept(this))).build());
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
