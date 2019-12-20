package edu.cornell.cs.apl.viaduct.backend.mamba.visitors;

import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaArrayDeclarationNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaArrayStoreNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaAssignNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaBlockNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaIfNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaInputNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaOutputNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaRegIntDeclarationNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaSecurityType;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaStatementNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaVariable;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaWhileNode;
import io.vavr.collection.HashSet;
import io.vavr.collection.Set;

public class MambaSecretVariablesVisitor implements MambaStatementVisitor<Set<MambaVariable>> {
  public static Set<MambaVariable> run(MambaStatementNode stmt) {
    return stmt.accept(new MambaSecretVariablesVisitor());
  }

  private MambaSecretVariablesVisitor() {}

  @Override
  public Set<MambaVariable> visit(MambaRegIntDeclarationNode node) {
    if (node.getRegisterType() == MambaSecurityType.SECRET) {
      return HashSet.of(node.getVariable());

    } else {
      return HashSet.empty();
    }
  }

  @Override
  public Set<MambaVariable> visit(MambaArrayDeclarationNode node) {
    if (node.getRegisterType() == MambaSecurityType.SECRET) {
      return HashSet.of(node.getVariable());

    } else {
      return HashSet.empty();
    }
  }

  @Override
  public Set<MambaVariable> visit(MambaAssignNode node) {
    return HashSet.empty();
  }

  @Override
  public Set<MambaVariable> visit(MambaArrayStoreNode node) {
    return HashSet.empty();
  }

  @Override
  public Set<MambaVariable> visit(MambaInputNode node) {
    if (node.getSecurityContext() == MambaSecurityType.SECRET) {
      return HashSet.of(node.getVariable());

    } else {
      return HashSet.empty();
    }
  }

  @Override
  public Set<MambaVariable> visit(MambaOutputNode node) {
    return HashSet.empty();
  }

  @Override
  public Set<MambaVariable> visit(MambaIfNode node) {
    return node.getThenBranch().accept(this).union(node.getElseBranch().accept(this));
  }

  @Override
  public Set<MambaVariable> visit(MambaWhileNode node) {
    return node.getBody().accept(this);
  }

  @Override
  public Set<MambaVariable> visit(MambaBlockNode node) {
    Set<MambaVariable> blockVars = HashSet.empty();

    for (MambaStatementNode stmt : node) {
      blockVars = blockVars.union(stmt.accept(this));
    }

    return blockVars;
  }
}
