package edu.cornell.cs.apl.viaduct.imp.visitors;

/** Visitor for any AST node. */
public interface AstVisitor<R>
    extends ExprVisitor<R>, StmtVisitor<R>, ProcessConfigurationVisitor<R> {}
