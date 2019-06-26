package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.imp.ast.ArrayIndex;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;

/** Reference visitor. */
public interface ReferenceVisitor<R> {
  R visit(Variable variable);

  R visit(ArrayIndex arrayIndex);
}
