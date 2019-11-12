package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.imp.ast.ArrayIndexingNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReferenceNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import java.util.Map;

public class RenameVisitor extends IdentityProgramVisitor {
  private final Map<Variable, Variable> variableMap;
  private final RenameReferenceVisitor referenceVisitor = new RenameReferenceVisitor();

  public RenameVisitor(Map<Variable, Variable> varMap) {
    this.variableMap = varMap;
  }

  @Override
  protected ReferenceVisitor<ReferenceNode> getReferenceVisitor() {
    return this.referenceVisitor;
  }

  protected class RenameReferenceVisitor
      extends AbstractReferenceVisitor<RenameReferenceVisitor, ReferenceNode, ExpressionNode> {

    @Override
    protected ExprVisitor<ExpressionNode> getExpressionVisitor() {
      return RenameVisitor.this.getExpressionVisitor();
    }

    @Override
    protected RenameReferenceVisitor enter(ReferenceNode node) {
      return this;
    }

    @Override
    protected ReferenceNode leave(Variable node, RenameReferenceVisitor visitor) {
      if (RenameVisitor.this.variableMap.containsKey(node)) {
        return RenameVisitor.this.variableMap.get(node);

      } else {
        return node;
      }
    }

    @Override
    protected ReferenceNode leave(
        ArrayIndexingNode node,
        RenameReferenceVisitor visitor,
        ReferenceNode array, ExpressionNode index)
    {
      return
          node.toBuilder()
          .setArray((Variable) array)
          .setIndex(index)
          .build();
    }
  }
}
