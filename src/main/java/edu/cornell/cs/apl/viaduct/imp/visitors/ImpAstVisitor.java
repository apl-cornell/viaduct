package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReferenceNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StatementNode;
import edu.cornell.cs.apl.viaduct.imp.ast.TopLevelDeclarationNode;

/** Visitor for pattern matching on the type of an AST node. */
public interface ImpAstVisitor<R> {
  R visit(ReferenceNode referenceNode);

  R visit(ExpressionNode expressionNode);

  R visit(StatementNode statementNode);

  R visit(TopLevelDeclarationNode declarationNode);

  R visit(ProgramNode programNode);
}
