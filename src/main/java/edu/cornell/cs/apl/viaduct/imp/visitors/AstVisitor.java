package edu.cornell.cs.apl.viaduct.imp.visitors;

/** visitor for any AST node. */
public interface AstVisitor<R> extends ExprVisitor<R>, StmtVisitor<R> {}
