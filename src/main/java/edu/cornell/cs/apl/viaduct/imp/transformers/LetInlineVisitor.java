package edu.cornell.cs.apl.viaduct.imp.transformers;

import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LetBindingNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReadNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StatementNode;
import edu.cornell.cs.apl.viaduct.imp.visitors.ReplaceVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;

// TODO: Inlining variables changes (most likely reduces) security. Change compilation and remove.

/** Inline let statements. */
class LetInlineVisitor extends ReplaceVisitor {
  private final StmtVisitor<StatementNode> statementVisitor = new InlineStmtVisitor();

  @Override
  protected StmtVisitor<StatementNode> getStatementVisitor() {
    return statementVisitor;
  }

  protected class InlineStmtVisitor extends ReplaceStmtVisitor {
    @Override
    protected StatementNode leave(
        LetBindingNode node, ReplaceStmtVisitor visitor, ExpressionNode rhs) {
      // Add a mapping from defined variable to expression
      final ExpressionNode var = ReadNode.builder().setReference(node.getVariable()).build();
      LetInlineVisitor.this.exprReplacements.put(var, rhs);

      // Remove this node
      return BlockNode.empty();
    }
  }
}
