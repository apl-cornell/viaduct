package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.imp.ast.ArrayAccessNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BinaryExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.DowngradeNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LiteralNode;
import edu.cornell.cs.apl.viaduct.imp.ast.NotNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReadNode;

/** Expression visitor. */
public interface ExprVisitor<R> {
  R visit(LiteralNode literalNode);

  R visit(ReadNode readNode);

  R visit(NotNode notNode);

  R visit(BinaryExpressionNode binaryExpressionNode);

  R visit(DowngradeNode downgradeNode);

  R visit(ArrayAccessNode arrAccessNode);
}
