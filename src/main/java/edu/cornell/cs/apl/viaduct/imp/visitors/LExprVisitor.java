package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.imp.ast.ArrayIndexNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LReadNode;

/** left-expression visitor. */
public interface LExprVisitor<R> {
  R visit(LReadNode lreadNode);

  R visit(ArrayIndexNode arrIndexNode);
}
