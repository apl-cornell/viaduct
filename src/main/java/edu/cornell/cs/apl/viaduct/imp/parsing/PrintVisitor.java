package edu.cornell.cs.apl.viaduct.imp.parsing;

import edu.cornell.cs.apl.viaduct.AstNode;
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
import edu.cornell.cs.apl.viaduct.protocol.Protocol;
import edu.cornell.cs.apl.viaduct.security.Label;
import edu.cornell.cs.apl.viaduct.util.PrintUtil;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

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

  private final boolean colorEnabled;

  /** Accumulates the partially printed program. */
  private final PrintStream output;

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
   * @param colorEnabled print semicolons (;) only if set to {@code true}
   */
  PrintVisitor(PrintStream output, boolean statementTerminatorsEnabled, boolean colorEnabled) {
    this.output = output;
    this.statementTerminatorsEnabled = statementTerminatorsEnabled;
    this.colorEnabled = colorEnabled;
  }

  /**
   * Constructor a print visitor.
   *
   * @param output stream to print to
   * @param statementTerminatorsEnabled print semicolons (;) only if set to {@code true}
   */
  PrintVisitor(PrintStream output, boolean statementTerminatorsEnabled) {
    this(output, statementTerminatorsEnabled, true);
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
  private void addSourceLocation(HasLocation node) {
    if (sourceLocationsEnabled
        && this.statementTerminatorsEnabled
        && node.getSourceLocation() != null) {
      addIndentation();
      printComment(node.getSourceLocation().toString());
      output.println();
    }
  }

  /** Toggle between printing with color or not. */
  private void printToggleColor(Ansi colorStr, String str) {
    if (this.colorEnabled) {
      output.print(colorStr);

    } else {
      output.print(str);
    }
  }

  /** Print a name. */
  public void print(Name name) {
    printToggleColor(
        Ansi.ansi().fg(Color.BLUE).a(name.getName()).reset(),
        name.getName());
  }

  /** Print a literal constant. */
  public void print(ImpValue value) {
    printToggleColor(
        Ansi.ansi().fg(Color.CYAN).a(value).reset(),
        value.toString());
  }

  /** Print a type. */
  public void print(ImpType type) {
    printToggleColor(
        Ansi.ansi().fg(Color.YELLOW).a(type).reset(),
        type.toString());
  }

  /** Print a label. */
  public void print(Label label) {
    printToggleColor(
        Ansi.ansi().fg(Color.YELLOW).a(label).reset(),
        label.toString());
  }

  /** Print a comment. */
  private void printComment(String comment) {
    printToggleColor(
        Ansi.ansi().fgBright(Color.GREEN).a("/* ").a(comment).a(" */").reset(),
        comment);
  }

  /** Print a builtin keyword. */
  private void printKeyword(String keyword) {
    printToggleColor(
        Ansi.ansi().fg(Color.GREEN).a(keyword).reset(),
        keyword);
  }

  /** Print a block node without adding indentation before the opening brace. */
  private void printChildBlock(BlockNode node) {
    output.println("{");

    indentation += PrintUtil.INDENTATION_LEVEL;
    for (StatementNode stmt : node) {
      stmt.accept(this);

      if (this.indentationEnabled) {
        output.println();
      }
    }
    indentation -= PrintUtil.INDENTATION_LEVEL;

    addIndentation();
    output.print('}');
  }

  @Override
  public Void visit(Variable variable) {
    print(variable);
    return null;
  }

  @Override
  public Void visit(ArrayIndexingNode arrayIndexingNode) {
    arrayIndexingNode.getArray().accept(this);
    output.print("[");
    arrayIndexingNode.getIndex().accept(this);
    output.print("]");
    return null;
  }

  @Override
  public Void visit(LiteralNode literalNode) {
    print(literalNode.getValue());
    return null;
  }

  @Override
  public Void visit(ReadNode readNode) {
    readNode.getReference().accept(this);
    return null;
  }

  @Override
  public Void visit(NotNode notNode) {
    output.print('!');
    return notNode.getExpression().accept(this);
  }

  @Override
  public Void visit(BinaryExpressionNode binaryExpressionNode) {
    output.print('(');
    binaryExpressionNode.getLhs().accept(this);

    output.print(' ');
    output.print(binaryExpressionNode.getOperator());
    output.print(' ');

    binaryExpressionNode.getRhs().accept(this);
    output.print(')');

    return null;
  }

  @Override
  public Void visit(DowngradeNode downgradeNode) {
    printKeyword(downgradeNode.getDowngradeType().toString());
    output.print("(");
    downgradeNode.getExpression().accept(this);
    output.print(", ");

    final Label fromLabel = downgradeNode.getFromLabel();
    if (fromLabel != null) {
      print(fromLabel);
      printKeyword(" to ");
    }
    print(downgradeNode.getToLabel());

    output.print(")");
    return null;
  }

  @Override
  public Void visit(VariableDeclarationNode varDeclNode) {
    addSourceLocation(varDeclNode);
    addIndentation();

    print(varDeclNode.getType());

    final Label label = varDeclNode.getLabel();
    if (label != null) {
      print(label);
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

    print(arrayDecl.getElementType());

    final Label label = arrayDecl.getLabel();
    if (label != null) {
      print(label);
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

    print(receiveNode.getVariable());
    output.print(" <- ");
    printKeyword("recv ");

    final ImpType recvType = receiveNode.getReceiveType();
    if (recvType != null) {
      print(recvType);
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
    output.print(" (");
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

    List<StatementNode> initList = new ArrayList<>();
    for (StatementNode initStmt : forNode.getInitialize()) {
      initList.add(initStmt);
    }
    if (initList.size() == 1) {
      initList.get(0).accept(this);

    } else {
      output.print("{");
      for (StatementNode initStmt : initList) {
        initStmt.accept(this);
        output.print(";");
      }
      output.print("}");
    }

    output.print("; ");
    forNode.getGuard().accept(this);
    output.print("; ");

    List<StatementNode> updateList = new ArrayList<>();
    for (StatementNode updateStmt : forNode.getUpdate()) {
      updateList.add(updateStmt);
    }
    if (updateList.size() == 1) {
      updateList.get(0).accept(this);

    } else {
      output.print("{");
      for (StatementNode updateStmt : updateList) {
        updateStmt.accept(this);
        output.print("; ");
      }
      output.print("}");
    }
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
    if (breakNode.getJumpLabel() != null) {
      output.print(' ');
      print(breakNode.getJumpLabel());
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

    Protocol<? extends AstNode> processProtocol = processDeclarationNode.getProtocol();
    if (processProtocol != null) {
      output.print(" : ");
      output.print(processProtocol.toString());
      output.print(" ");

    } else {
    output.print(' ');
    }

    printChildBlock(processDeclarationNode.getBody());

    return null;
  }

  @Override
  public Void visit(HostDeclarationNode hostDeclarationNode) {
    addSourceLocation(hostDeclarationNode);

    printKeyword("host ");
    output.print(hostDeclarationNode.getName());
    output.print(" : ");
    print(hostDeclarationNode.getTrust());
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
