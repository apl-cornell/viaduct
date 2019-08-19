package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.AstPrinter;
import edu.cornell.cs.apl.viaduct.imp.ast.ArrayDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ArrayIndex;
import edu.cornell.cs.apl.viaduct.imp.ast.AssertNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssignNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BinaryExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BreakNode;
import edu.cornell.cs.apl.viaduct.imp.ast.DowngradeNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ForNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Host;
import edu.cornell.cs.apl.viaduct.imp.ast.IfNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpType;
import edu.cornell.cs.apl.viaduct.imp.ast.LetBindingNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LiteralNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LoopNode;
import edu.cornell.cs.apl.viaduct.imp.ast.NotNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessName;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReadNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReceiveNode;
import edu.cornell.cs.apl.viaduct.imp.ast.SendNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.imp.ast.VariableDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.WhileNode;
import edu.cornell.cs.apl.viaduct.security.Label;
import io.vavr.Tuple2;

/** Pretty-prints an AST. */
public class PrintVisitor
    implements ReferenceVisitor<Void>, ExprVisitor<Void>,
        StmtVisitor<Void>, ProgramVisitor<Void>,
        AstPrinter<ImpAstNode> {

  private static final int INDENTATION_LEVEL = 2;

  /** Accumulates the partially printed program. */
  private StringBuilder buffer;

  /** Add indentation only if set to true. */
  private boolean indentationEnabled;

  /** Terminate statements with a semicolon (;) only if set to true. */
  private boolean statementTerminatorsEnabled;

  /** Current indentation level. */
  private int indentation = 0;

  /** constructor. */
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
  public static String run(StmtNode stmt) {
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

  /** Append current indentation to the buffer. */
  private void addIndentation() {
    if (this.indentationEnabled) {
      for (int i = 0; i < this.indentation; i++) {
        buffer.append(' ');
      }
    }
  }

  private void addSeparator() {
    if (this.statementTerminatorsEnabled) {
      buffer.append(';');
    }
  }

  @Override
  public String print(ImpAstNode astNode) {
    if (astNode instanceof ExpressionNode) {
      ((ExpressionNode)astNode).accept(this);

    } else if (astNode instanceof StmtNode) {
      ((StmtNode)astNode).accept(this);

    } else {
      ((ProgramNode)astNode).accept(this);
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
  public Void visit(ArrayIndex arrayIndex) {
    buffer.append(arrayIndex.getArray());
    buffer.append("[");
    arrayIndex.getIndex().accept(this);
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
    // TODO: special case declassify and endorse
    buffer.append("downgrade(");
    downgradeNode.getExpression().accept(this);
    buffer.append(", ");
    buffer.append(downgradeNode.getLabel());
    buffer.append(")");
    return null;
  }

  @Override
  public Void visit(VariableDeclarationNode varDeclNode) {
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
    addIndentation();

    buffer.append(arrayDecl.getType());

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
    addIndentation();

    assignNode.getLhs().accept(this);
    buffer.append(" = ");
    assignNode.getRhs().accept(this);

    addSeparator();
    return null;
  }

  @Override
  public Void visit(SendNode sendNode) {
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
    addIndentation();

    buffer.append(receiveNode.getVariable());
    buffer.append(" <- recv ");

    ImpType recvType = receiveNode.getRecvType();
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
    addIndentation();

    buffer.append("assert ");
    assertNode.getExpression().accept(this);

    addSeparator();
    return null;
  }

  @Override
  public Void visit(IfNode ifNode) {
    addIndentation();

    buffer.append("if (");
    ifNode.getGuard().accept(this);
    buffer.append(") ");

    ifNode.getThenBranch().accept(this);

    StmtNode elseBranch = ifNode.getElseBranch();
    boolean elseEmpty = elseBranch instanceof BlockNode && ((BlockNode) elseBranch).size() == 0;
    if (!elseEmpty) {
      buffer.append(" else ");
      ifNode.getElseBranch().accept(this);
    }

    return null;
  }

  @Override
  public Void visit(WhileNode whileNode) {
    addIndentation();

    buffer.append("while (");
    whileNode.getGuard().accept(this);
    buffer.append(") ");

    whileNode.getBody().accept(this);

    return null;
  }

  @Override
  public Void visit(ForNode forNode) {
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
    addIndentation();
    buffer.append("loop ");
    loopNode.getBody().accept(this);
    return null;
  }

  @Override
  public Void visit(BreakNode breakNode) {
    addIndentation();
    buffer.append("break ");
    breakNode.getLevel().accept(this);
    addSeparator();
    return null;
  }

  @Override
  public Void visit(BlockNode blockNode) {
    buffer.append("{\n");

    indentation += INDENTATION_LEVEL;
    for (StmtNode stmt : blockNode) {
      stmt.accept(this);
      buffer.append('\n');
    }
    indentation -= INDENTATION_LEVEL;

    addIndentation();
    buffer.append('}');

    return null;
  }

  @Override
  public Void visit(ProgramNode programNode) {
    boolean first = true;

    for (Tuple2<ProcessName, StmtNode> process : programNode) {
      if (!first) {
        buffer.append("\n\n");
      }

      final ProcessName processName = process._1();
      final StmtNode statement = process._2();
      buffer.append("process ");
      buffer.append(processName);
      buffer.append(' ');
      statement.accept(this);

      first = false;
    }

    if (!first) {
      buffer.append("\n");
    }

    for (Tuple2<Host, Label> host : programNode.getHostTrustConfiguration()) {
      if (!first) {
        buffer.append("\n");
      }

      final Host hostName = host._1();
      final Label trust = host._2();
      buffer.append("host ");
      buffer.append(hostName);
      buffer.append(" : ");
      buffer.append(trust);
      buffer.append(";");

      first = false;
    }

    return null;
  }
}
