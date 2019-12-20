package edu.cornell.cs.apl.viaduct.imp.parsing;

import edu.cornell.cs.apl.viaduct.AstPrinter;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Name;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReferenceNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StatementNode;
import edu.cornell.cs.apl.viaduct.imp.ast.TopLevelDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.types.ImpType;
import edu.cornell.cs.apl.viaduct.imp.ast.values.ImpValue;
import edu.cornell.cs.apl.viaduct.imp.visitors.ImpAstVisitor;
import edu.cornell.cs.apl.viaduct.security.Label;
import edu.cornell.cs.apl.viaduct.util.PrintUtil;
import java.io.PrintStream;

/** Pretty-prints an AST. */
public final class Printer implements AstPrinter<ImpAstNode> {
  /**
   * Pretty print an AST node to the given output stream.
   *
   * @param node AST node to print
   * @param output output stream that can handle ANSI colors
   * @param colorEnabled toggle coloring keywords
   */
  public static void run(ImpAstNode node, PrintStream output, boolean colorEnabled) {
    node.accept(
        new ImpAstVisitor<Void>() {
          @Override
          public Void visit(ReferenceNode referenceNode) {
            return referenceNode.accept(new PrintVisitor(output, false, colorEnabled));
          }

          @Override
          public Void visit(ExpressionNode expressionNode) {
            return expressionNode.accept(new PrintVisitor(output, false, colorEnabled));
          }

          @Override
          public Void visit(StatementNode statementNode) {
            return statementNode.accept(new PrintVisitor(output, false, colorEnabled));
          }

          @Override
          public Void visit(TopLevelDeclarationNode declarationNode) {
            return declarationNode.accept(new PrintVisitor(output, true, colorEnabled));
          }

          @Override
          public Void visit(ProgramNode programNode) {
            programNode.accept(new PrintVisitor(output, true, colorEnabled));
            output.println();
            return null;
          }
        });
  }

  /** Pretty print an AST node to the given output stream. */
  public static void run(ImpAstNode node, PrintStream output) {
    run(node, output, true);
  }

  /** Pretty print a name. */
  public static void run(Name name, PrintStream output) {
    new PrintVisitor(output, false).print(name);
  }

  /** Pretty print a literal constant. */
  public static void run(ImpValue value, PrintStream output) {
    new PrintVisitor(output, false).print(value);
  }

  /** Pretty print a type. */
  public static void run(ImpType type, PrintStream output) {
    new PrintVisitor(output, false).print(type);
  }

  /** Pretty print a label. */
  public static void run(Label label, PrintStream output) {
    new PrintVisitor(output, false).print(label);
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
