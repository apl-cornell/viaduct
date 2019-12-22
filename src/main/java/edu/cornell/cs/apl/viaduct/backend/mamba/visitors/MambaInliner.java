package edu.cornell.cs.apl.viaduct.backend.mamba.visitors;

import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaExpressionNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaReadNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaStatementNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaVariable;
import io.vavr.collection.Map;

/** inline expressions by substituting variables. */
public class MambaInliner extends MambaIdentityVisitor {
  private final Map<MambaVariable, MambaExpressionNode> inlineMap;

  private MambaInliner(Map<MambaVariable, MambaExpressionNode> inlineMap) {
    this.inlineMap = inlineMap;
  }

  public static MambaExpressionNode run(
      Map<MambaVariable, MambaExpressionNode> inlineMap, MambaExpressionNode expr) {
    return expr.accept(new MambaInliner(inlineMap));
  }

  public static MambaStatementNode run(
      Map<MambaVariable, MambaExpressionNode> inlineMap, MambaStatementNode stmt) {
    return stmt.accept(new MambaInliner(inlineMap));
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
}
