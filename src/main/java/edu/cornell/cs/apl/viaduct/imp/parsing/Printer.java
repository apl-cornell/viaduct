package edu.cornell.cs.apl.viaduct.imp.parsing;

import edu.cornell.cs.apl.viaduct.AstPrinter;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReferenceNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StatementNode;
import edu.cornell.cs.apl.viaduct.imp.ast.TopLevelDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.visitors.ImpAstVisitor;
import edu.cornell.cs.apl.viaduct.util.PrintUtil;
import java.io.PrintStream;

/** Pretty-prints an AST. */
public final class Printer implements AstPrinter<ImpAstNode> {
  /**
   * Pretty print an AST node to the given output stream (with colors).
   *
   * @param node AST node to print
   * @param output output stream that can handle ANSI colors
   */
  public static void run(ImpAstNode node, PrintStream output) {
    node.accept(
        new ImpAstVisitor<Void>() {
          @Override
          public Void visit(ReferenceNode referenceNode) {
            return referenceNode.accept(new PrintVisitor(output, false));
          }

          @Override
          public Void visit(ExpressionNode expressionNode) {
            return expressionNode.accept(new PrintVisitor(output, false));
          }

          @Override
          public Void visit(StatementNode statementNode) {
            return statementNode.accept(new PrintVisitor(output, false));
          }

          @Override
          public Void visit(TopLevelDeclarationNode declarationNode) {
            return declarationNode.accept(new PrintVisitor(output, true));
          }

          @Override
          public Void visit(ProgramNode programNode) {
            programNode.accept(new PrintVisitor(output, true));
            output.println();
            return null;
          }
        });
  }

  /** Pretty print an AST node and return the result as a string. */
  public static String run(ImpAstNode node) {
    return PrintUtil.printToString(output -> run(node, output));
  }

  @Override
  public String print(ImpAstNode node) {
    return run(node);
  }
}
