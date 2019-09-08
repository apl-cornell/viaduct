package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.AstPrinter;
import edu.cornell.cs.apl.viaduct.imp.ast.ArrayDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ArrayIndexingNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssertNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssignNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BinaryExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BreakNode;
import edu.cornell.cs.apl.viaduct.imp.ast.DowngradeNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ForNode;
import edu.cornell.cs.apl.viaduct.imp.ast.HostDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.IfNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LetBindingNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LiteralNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LoopNode;
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
import edu.cornell.cs.apl.viaduct.imp.parser.Located;
import edu.cornell.cs.apl.viaduct.security.Label;
import edu.cornell.cs.apl.viaduct.util.PrintUtil;

/** Pretty-prints an AST. */
public class PrintVisitor
    implements ReferenceVisitor<Void>,
        ExprVisitor<Void>,
        StmtVisitor<Void>,
        TopLevelDeclarationVisitor<Void>,
        ProgramVisitor<Void>,
        AstPrinter<ImpAstNode> {

  /** Print source locations only if set to true. */
  private static final boolean sourceLocationsEnabled = false;

  /** Accumulates the partially printed program. */
  private StringBuilder buffer;

  /** Add indentation only if set to true. */
  private boolean indentationEnabled;

  /** Terminate statements with a semicolon (;) only if set to true. */
  private boolean statementTerminatorsEnabled;

  /** Current indentation level. */
  private int indentation = 0;

  /**
   * Constructor a printer.
   *
   * @param statementTerminatorsEnabled print semicolons (;) only if set to {@code true}
   */
  public PrintVisitor(boolean statementTerminatorsEnabled) {
    this.buffer = new StringBuilder();
    this.indentationEnabled = true;
    this.statementTerminatorsEnabled = statementTerminatorsEnabled;
  }

  /** Pretty print an expression. */
  public static String run(ExpressionNode expr) {
    final PrintVisitor v = new PrintVisitor(false);
    expr.accept(v);
    return v.buffer.toString();
  }

  /** Pretty print a statement. */
  public static String run(StatementNode stmt) {
    final PrintVisitor v = new PrintVisitor(false);
    stmt.accept(v);
    return v.buffer.toString();
  }

  /** Pretty print a program. */
  public static String run(ProgramNode prog) {
    final PrintVisitor v = new PrintVisitor(true);
    prog.accept(v);
    return v.buffer.toString();
  }

  /** Append current indentation to the buffer (if indentation is enabled). */
  private void addIndentation() {
    if (this.indentationEnabled) {
      for (int i = 0; i < this.indentation; i++) {
        buffer.append(' ');
      }
    }
  }

  /** Print a statement terminator (if terminators are enabled). */
  private void addSeparator() {
    if (this.statementTerminatorsEnabled) {
      buffer.append(';');
    }
  }

  /**
   * Print the source location of the given node on a new line (if source locations are enabled).
   */
  private void addSourceLocation(Located node) {
    if (this.sourceLocationsEnabled
        && this.statementTerminatorsEnabled
        && node.getSourceLocation() != null) {
      addIndentation();
      buffer.append("/* ");
      buffer.append(node.getSourceLocation());
      buffer.append(" */ ");
      buffer.append("\n");
    }
  }

  @Override
  public String print(ImpAstNode astNode) {
    // TODO: use ImpAstNodeVisitor
    if (astNode instanceof ExpressionNode) {
      ((ExpressionNode) astNode).accept(this);

    } else if (astNode instanceof StatementNode) {
      ((StatementNode) astNode).accept(this);

    } else {
      ((ProgramNode) astNode).accept(this);
    }

    String str = this.buffer.toString();
    this.buffer = new StringBuilder();
    return str;
  }

  @Override
  public Void visit(Variable variable) {
    buffer.append(variable);
    return null;
  }

  @Override
  public Void visit(ArrayIndexingNode arrayIndexingNode) {
    buffer.append(arrayIndexingNode.getArray());
    buffer.append("[");
    arrayIndexingNode.getIndex().accept(this);
    buffer.append("]");
    return null;
  }

  @Override
  public Void visit(LiteralNode literalNode) {
    buffer.append(literalNode.getValue());
    return null;
  }

  @Override
  public Void visit(ReadNode readNode) {
    readNode.getReference().accept(this);
    return null;
  }

  @Override
  public Void visit(NotNode notNode) {
    buffer.append('!');
    return notNode.getExpression().accept(this);
  }

  @Override
  public Void visit(BinaryExpressionNode binaryExpressionNode) {
    buffer.append('(');
    binaryExpressionNode.getLhs().accept(this);

    buffer.append(' ');
    buffer.append(binaryExpressionNode.getOperator());
    buffer.append(' ');

    binaryExpressionNode.getRhs().accept(this);
    buffer.append(')');

    return null;
  }

  @Override
  public Void visit(DowngradeNode downgradeNode) {
    switch (downgradeNode.getDowngradeType()) {
      case ENDORSE:
        buffer.append("endorse");
        break;

      case DECLASSIFY:
        buffer.append("declassify");
        break;

      case BOTH:
        buffer.append("downgrade");
        break;

      default:
        break;
    }
    buffer.append("(");
    downgradeNode.getExpression().accept(this);
    buffer.append(", ");

    final Label fromLabel = downgradeNode.getFromLabel();
    if (fromLabel != null) {
      buffer.append(fromLabel);
      buffer.append(" to ");
    }
    buffer.append(downgradeNode.getToLabel());

    buffer.append(")");
    return null;
  }

  @Override
  public Void visit(VariableDeclarationNode varDeclNode) {
    addSourceLocation(varDeclNode);
    addIndentation();

    buffer.append(varDeclNode.getType());

    Label label = varDeclNode.getLabel();
    if (label != null) {
      buffer.append(label);
    }

    buffer.append(" ");
    buffer.append(varDeclNode.getVariable());

    addSeparator();
    return null;
  }

  @Override
  public Void visit(ArrayDeclarationNode arrayDecl) {
    addSourceLocation(arrayDecl);
    addIndentation();

    buffer.append(arrayDecl.getElementType());

    Label label = arrayDecl.getLabel();
    if (label != null) {
      buffer.append(label);
    }

    buffer.append(" ");
    buffer.append(arrayDecl.getVariable());

    buffer.append('[');
    arrayDecl.getLength().accept(this);
    buffer.append(']');

    addSeparator();
    return null;
  }

  @Override
  public Void visit(LetBindingNode letBindingNode) {
    addSourceLocation(letBindingNode);
    addIndentation();

    buffer.append("let ");
    buffer.append(letBindingNode.getVariable());
    buffer.append(" = ");
    letBindingNode.getRhs().accept(this);

    addSeparator();
    return null;
  }

  @Override
  public Void visit(AssignNode assignNode) {
    addSourceLocation(assignNode);
    addIndentation();

    assignNode.getLhs().accept(this);
    buffer.append(" = ");
    assignNode.getRhs().accept(this);

    addSeparator();
    return null;
  }

  @Override
  public Void visit(SendNode sendNode) {
    addSourceLocation(sendNode);
    addIndentation();

    buffer.append("send ");
    sendNode.getSentExpression().accept(this);
    buffer.append(" to ");
    buffer.append(sendNode.getRecipient());

    addSeparator();
    return null;
  }

  @Override
  public Void visit(ReceiveNode receiveNode) {
    addSourceLocation(receiveNode);
    addIndentation();

    buffer.append(receiveNode.getVariable());
    buffer.append(" <- recv ");

    ImpType recvType = receiveNode.getReceiveType();
    if (recvType != null) {
      buffer.append(recvType);
      buffer.append(" ");
    }

    buffer.append(receiveNode.getSender());

    addSeparator();
    return null;
  }

  @Override
  public Void visit(AssertNode assertNode) {
    addSourceLocation(assertNode);
    addIndentation();

    buffer.append("assert ");
    assertNode.getExpression().accept(this);

    addSeparator();
    return null;
  }

  @Override
  public Void visit(IfNode ifNode) {
    addSourceLocation(ifNode);
    addIndentation();

    buffer.append("if (");
    ifNode.getGuard().accept(this);
    buffer.append(") ");

    ifNode.getThenBranch().accept(this);

    if (ifNode.getElseBranch().getStatements().size() > 0) {
      buffer.append(" else ");
      ifNode.getElseBranch().accept(this);
    }

    return null;
  }

  @Override
  public Void visit(WhileNode whileNode) {
    addSourceLocation(whileNode);
    addIndentation();

    buffer.append("while (");
    whileNode.getGuard().accept(this);
    buffer.append(") ");

    whileNode.getBody().accept(this);

    return null;
  }

  @Override
  public Void visit(ForNode forNode) {
    addSourceLocation(forNode);
    addIndentation();

    this.indentationEnabled = false;
    this.statementTerminatorsEnabled = false;

    buffer.append("for (");
    forNode.getInitialize().accept(this);
    buffer.append("; ");
    forNode.getGuard().accept(this);
    buffer.append("; ");
    forNode.getUpdate().accept(this);
    buffer.append(")");

    this.indentationEnabled = true;
    this.statementTerminatorsEnabled = true;

    forNode.getBody().accept(this);
    return null;
  }

  @Override
  public Void visit(LoopNode loopNode) {
    addSourceLocation(loopNode);
    addIndentation();

    buffer.append("loop ");
    loopNode.getBody().accept(this);

    return null;
  }

  @Override
  public Void visit(BreakNode breakNode) {
    addSourceLocation(breakNode);
    addIndentation();

    buffer.append("break");
    if (breakNode.getLevel() > 1) {
      buffer.append(' ');
      buffer.append(breakNode.getLevel());
    }
    addSeparator();

    return null;
  }

  @Override
  public Void visit(BlockNode blockNode) {
    buffer.append("{\n");

    indentation += PrintUtil.INDENTATION_LEVEL;
    for (StatementNode stmt : blockNode) {
      stmt.accept(this);
      buffer.append('\n');
    }
    indentation -= PrintUtil.INDENTATION_LEVEL;

    addIndentation();
    buffer.append('}');

    return null;
  }

  @Override
  public Void visit(ProcessDeclarationNode processDeclarationNode) {
    addSourceLocation(processDeclarationNode);

    buffer.append("process ");
    buffer.append(processDeclarationNode.getName());
    buffer.append(' ');
    processDeclarationNode.getBody().accept(this);

    return null;
  }

  @Override
  public Void visit(HostDeclarationNode hostDeclarationNode) {
    addSourceLocation(hostDeclarationNode);

    buffer.append("host ");
    buffer.append(hostDeclarationNode.getName());
    buffer.append(" : ");
    buffer.append(hostDeclarationNode.getTrust());
    buffer.append(";");

    return null;
  }

  @Override
  public Void visit(ProgramNode programNode) {
    boolean first = true;
    for (TopLevelDeclarationNode declaration : programNode) {
      if (!first) {
        buffer.append("\n\n");
      }

      declaration.accept(this);

      first = false;
    }

    return null;
  }
}
