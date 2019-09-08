package edu.cornell.cs.apl.viaduct.imp.parsing;

import edu.cornell.cs.apl.viaduct.imp.ast.ArrayDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ArrayIndexingNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssertNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssignNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BinaryExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BreakNode;
import edu.cornell.cs.apl.viaduct.imp.ast.DowngradeNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ForNode;
import edu.cornell.cs.apl.viaduct.imp.ast.HostDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.IfNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LetBindingNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LiteralNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LoopNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Name;
import edu.cornell.cs.apl.viaduct.imp.ast.NotNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReadNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReceiveNode;
import edu.cornell.cs.apl.viaduct.imp.ast.SendNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StatementNode;
import edu.cornell.cs.apl.viaduct.imp.ast.TopLevelDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.imp.ast.VariableDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.WhileNode;
import edu.cornell.cs.apl.viaduct.imp.ast.types.ImpType;
import edu.cornell.cs.apl.viaduct.imp.ast.values.ImpValue;
import edu.cornell.cs.apl.viaduct.imp.visitors.ExprVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.ProgramVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.ReferenceVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.TopLevelDeclarationVisitor;
import edu.cornell.cs.apl.viaduct.security.Label;
import edu.cornell.cs.apl.viaduct.util.PrintUtil;
import java.io.PrintStream;
import org.apache.commons.lang3.StringUtils;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;

final class PrintVisitor
    implements ReferenceVisitor<Void>,
        ExprVisitor<Void>,
        StmtVisitor<Void>,
        TopLevelDeclarationVisitor<Void>,
        ProgramVisitor<Void> {

  /** Print source locations only if set to true. */
  private static final boolean sourceLocationsEnabled = false;

  /** Accumulates the partially printed program. */
  private PrintStream output;

  /** Add indentation only if set to true. */
  private boolean indentationEnabled = true;

  /** Terminate statements with a semicolon (;) only if set to true. */
  private boolean statementTerminatorsEnabled;

  /** Current indentation level. */
  private int indentation = 0;

  /**
   * Constructor a print visitor.
   *
   * @param output stream to print to
   * @param statementTerminatorsEnabled print semicolons (;) only if set to {@code true}
   */
  PrintVisitor(PrintStream output, boolean statementTerminatorsEnabled) {
    this.output = output;
    this.statementTerminatorsEnabled = statementTerminatorsEnabled;
  }

  /** Append current indentation to the output (if indentation is enabled). */
  private void addIndentation() {
    if (indentationEnabled) {
      output.print(StringUtils.repeat(' ', this.indentation));
    }
  }

  /** Print a statement terminator (if terminators are enabled). */
  private void addSeparator() {
    if (this.statementTerminatorsEnabled) {
      output.print(';');
    }
  }

  /**
   * Print the source location of the given node on a new line (if source locations are enabled).
   */
  private void addSourceLocation(Located node) {
    if (sourceLocationsEnabled
        && this.statementTerminatorsEnabled
        && node.getSourceLocation() != null) {
      addIndentation();
      printComment(node.getSourceLocation().toString());
      output.println();
    }
  }

  /** Print a comment. */
  private void printComment(String comment) {
    output.print(Ansi.ansi().fgBright(Color.GREEN).a("/* ").a(comment).a(" */").reset());
  }

  /**
   * Print a literal constant. Use for things such as:
   *
   * <ul>
   *   <li>Strings: {@code "this is a string"}
   *   <li>Characters: {@code 'c', '\n'}
   *   <li>Numbers: {@code 234, 2.3e10, 0xff}
   *   <li>Booleans: {@code true, false}
   * </ul>
   */
  private void printConstant(ImpValue constant) {
    output.print(Ansi.ansi().fg(Color.CYAN).a(constant).reset());
  }

  /** Print a name. */
  private void printIdentifier(Name identifier) {
    output.print(Ansi.ansi().fg(Color.BLUE).a(identifier).reset());
  }

  /** Print a builtin keyword or operator. */
  private void printKeyword(Object keyword) {
    output.print(Ansi.ansi().fg(Color.GREEN).a(keyword).reset());
  }

  /**
   * Print a type annotation. Use for things such as {@code int}, {@code string}, security labels,
   * etc.
   */
  private void printTypeAnnotation(Object annotation) {
    output.print(Ansi.ansi().fg(Color.YELLOW).a(annotation).reset());
  }

  /** Print a block node without adding indentation before the opening brace. */
  private void printChildBlock(BlockNode node) {
    output.println("{");

    indentation += PrintUtil.INDENTATION_LEVEL;
    for (StatementNode stmt : node) {
      stmt.accept(this);
      output.println();
    }
    indentation -= PrintUtil.INDENTATION_LEVEL;

    addIndentation();
    output.print('}');
  }

  @Override
  public Void visit(Variable variable) {
    printIdentifier(variable);
    return null;
  }

  @Override
  public Void visit(ArrayIndexingNode arrayIndexingNode) {
    printIdentifier(arrayIndexingNode.getArray());
    output.print("[");
    arrayIndexingNode.getIndex().accept(this);
    output.print("]");
    return null;
  }

  @Override
  public Void visit(LiteralNode literalNode) {
    printConstant(literalNode.getValue());
    return null;
  }

  @Override
  public Void visit(ReadNode readNode) {
    readNode.getReference().accept(this);
    return null;
  }

  @Override
  public Void visit(NotNode notNode) {
    printKeyword('!');
    return notNode.getExpression().accept(this);
  }

  @Override
  public Void visit(BinaryExpressionNode binaryExpressionNode) {
    output.print('(');
    binaryExpressionNode.getLhs().accept(this);

    output.print(' ');
    printKeyword(binaryExpressionNode.getOperator());
    output.print(' ');

    binaryExpressionNode.getRhs().accept(this);
    output.print(')');

    return null;
  }

  @Override
  public Void visit(DowngradeNode downgradeNode) {
    printKeyword(downgradeNode.getDowngradeType());
    output.print("(");
    downgradeNode.getExpression().accept(this);
    output.print(", ");

    final Label fromLabel = downgradeNode.getFromLabel();
    if (fromLabel != null) {
      printTypeAnnotation(fromLabel);
      printKeyword(" to ");
    }
    printTypeAnnotation(downgradeNode.getToLabel());

    output.print(")");
    return null;
  }

  @Override
  public Void visit(VariableDeclarationNode varDeclNode) {
    addSourceLocation(varDeclNode);
    addIndentation();

    printTypeAnnotation(varDeclNode.getType());

    Label label = varDeclNode.getLabel();
    if (label != null) {
      printTypeAnnotation(label);
    }

    output.print(" ");
    varDeclNode.getVariable().accept(this);

    addSeparator();
    return null;
  }

  @Override
  public Void visit(ArrayDeclarationNode arrayDecl) {
    addSourceLocation(arrayDecl);
    addIndentation();

    printTypeAnnotation(arrayDecl.getElementType());

    Label label = arrayDecl.getLabel();
    if (label != null) {
      printTypeAnnotation(label);
    }

    output.print(" ");
    arrayDecl.getVariable().accept(this);

    output.print('[');
    arrayDecl.getLength().accept(this);
    output.print(']');

    addSeparator();
    return null;
  }

  @Override
  public Void visit(LetBindingNode letBindingNode) {
    addSourceLocation(letBindingNode);
    addIndentation();

    printKeyword("let ");
    letBindingNode.getVariable().accept(this);
    output.print(" = ");
    letBindingNode.getRhs().accept(this);

    addSeparator();
    return null;
  }

  @Override
  public Void visit(AssignNode assignNode) {
    addSourceLocation(assignNode);
    addIndentation();

    assignNode.getLhs().accept(this);
    output.print(" = ");
    assignNode.getRhs().accept(this);

    addSeparator();
    return null;
  }

  @Override
  public Void visit(SendNode sendNode) {
    addSourceLocation(sendNode);
    addIndentation();

    printKeyword("send ");
    sendNode.getSentExpression().accept(this);
    printKeyword(" to ");
    output.print(sendNode.getRecipient());

    addSeparator();
    return null;
  }

  @Override
  public Void visit(ReceiveNode receiveNode) {
    addSourceLocation(receiveNode);
    addIndentation();

    printIdentifier(receiveNode.getVariable());
    output.print(" <- ");
    printKeyword("recv ");

    final ImpType recvType = receiveNode.getReceiveType();
    if (recvType != null) {
      printTypeAnnotation(recvType);
      output.print(" ");
    }

    output.print(receiveNode.getSender());

    addSeparator();
    return null;
  }

  @Override
  public Void visit(AssertNode assertNode) {
    addSourceLocation(assertNode);
    addIndentation();

    printKeyword("assert ");
    assertNode.getExpression().accept(this);

    addSeparator();
    return null;
  }

  @Override
  public Void visit(IfNode ifNode) {
    addSourceLocation(ifNode);
    addIndentation();

    printKeyword("if");
    output.println(" (");
    ifNode.getGuard().accept(this);
    output.print(") ");

    printChildBlock(ifNode.getThenBranch());

    if (ifNode.getElseBranch().getStatements().size() > 0) {
      printKeyword(" else ");
      printChildBlock(ifNode.getElseBranch());
    }

    return null;
  }

  @Override
  public Void visit(WhileNode whileNode) {
    addSourceLocation(whileNode);
    addIndentation();

    printKeyword("while");
    output.print(" (");
    whileNode.getGuard().accept(this);
    output.print(") ");

    printChildBlock(whileNode.getBody());

    return null;
  }

  @Override
  public Void visit(ForNode forNode) {
    addSourceLocation(forNode);
    addIndentation();

    this.indentationEnabled = false;
    this.statementTerminatorsEnabled = false;

    printKeyword("for");
    output.print(" (");
    forNode.getInitialize().accept(this);
    output.print("; ");
    forNode.getGuard().accept(this);
    output.print("; ");
    forNode.getUpdate().accept(this);
    output.print(")");

    this.indentationEnabled = true;
    this.statementTerminatorsEnabled = true;

    printChildBlock(forNode.getBody());
    return null;
  }

  @Override
  public Void visit(LoopNode loopNode) {
    addSourceLocation(loopNode);
    addIndentation();

    printKeyword("loop ");
    printChildBlock(loopNode.getBody());

    return null;
  }

  @Override
  public Void visit(BreakNode breakNode) {
    addSourceLocation(breakNode);
    addIndentation();

    printKeyword("break");
    if (breakNode.getLevel() > 1) {
      output.print(' ');
      output.print(breakNode.getLevel());
    }
    addSeparator();

    return null;
  }

  @Override
  public Void visit(BlockNode blockNode) {
    addSourceLocation(blockNode);
    addIndentation();

    printChildBlock(blockNode);
    return null;
  }

  @Override
  public Void visit(ProcessDeclarationNode processDeclarationNode) {
    addSourceLocation(processDeclarationNode);

    printKeyword("process ");
    output.print(processDeclarationNode.getName());
    output.print(' ');
    printChildBlock(processDeclarationNode.getBody());

    return null;
  }

  @Override
  public Void visit(HostDeclarationNode hostDeclarationNode) {
    addSourceLocation(hostDeclarationNode);

    printKeyword("host ");
    output.print(hostDeclarationNode.getName());
    output.print(" : ");
    printTypeAnnotation(hostDeclarationNode.getTrust());
    output.print(";");

    return null;
  }

  @Override
  public Void visit(ProgramNode programNode) {
    boolean first = true;
    for (TopLevelDeclarationNode declaration : programNode) {
      if (!first) {
        output.print("\n\n");
      }

      declaration.accept(this);

      first = false;
    }

    return null;
  }
}
