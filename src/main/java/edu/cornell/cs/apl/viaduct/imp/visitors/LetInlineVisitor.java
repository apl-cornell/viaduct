package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LetBindingNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReadNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;

/** inline let statements. */
public class LetInlineVisitor extends ReplaceVisitor {
  @Override
  public StmtNode visit(LetBindingNode letBinding) {
    Variable var = letBinding.getVariable();
    ExpressionNode newRhs = letBinding.getRhs().accept(this);
    this.exprMap.put(ReadNode.create(var), newRhs);
    return BlockNode.create();
  }
}
