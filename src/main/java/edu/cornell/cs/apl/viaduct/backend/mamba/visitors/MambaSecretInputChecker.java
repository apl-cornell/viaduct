package edu.cornell.cs.apl.viaduct.backend.mamba.visitors;

import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaArrayLoadNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaBinaryExpressionNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaExpressionNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaIntLiteralNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaMuxNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaNegationNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaReadNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaRevealNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaVariable;

import io.vavr.collection.Set;

/** check if secret data is used in an expression. */
public class MambaSecretInputChecker implements MambaExpressionVisitor<Boolean> {
  private final Set<MambaVariable> secretVariables;

  public static boolean run(Set<MambaVariable> secretVars, MambaExpressionNode expr) {
    return expr.accept(new MambaSecretInputChecker(secretVars));
  }

  public MambaSecretInputChecker(Set<MambaVariable> secretVariables) {
    this.secretVariables = secretVariables;
  }

  @Override
  public Boolean visit(MambaIntLiteralNode node) {
    return false;
  }

  @Override
  public Boolean visit(MambaReadNode node) {
    return this.secretVariables.contains(node.getVariable());
  }

  @Override
  public Boolean visit(MambaArrayLoadNode node) {
    return this.secretVariables.contains(node.getArray());
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
