package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;

/** Process configuration visitor. */
public interface ProgramVisitor<R> {
  R visit(ProgramNode programNode);
}
