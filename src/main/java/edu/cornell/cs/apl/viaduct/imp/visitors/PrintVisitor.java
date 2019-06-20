package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.imp.ast.ArrayAccessNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ArrayDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssertNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssignNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BinaryExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.DeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.DowngradeNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Host;
import edu.cornell.cs.apl.viaduct.imp.ast.IfNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LiteralNode;
import edu.cornell.cs.apl.viaduct.imp.ast.NotNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessName;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReadNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReceiveNode;
import edu.cornell.cs.apl.viaduct.imp.ast.SendNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;
import edu.cornell.cs.apl.viaduct.imp.ast.WhileNode;
import edu.cornell.cs.apl.viaduct.security.Label;
import io.vavr.Tuple2;

/** Pretty-prints an AST. */
public class PrintVisitor implements AstVisitor<Void> {
  private static final int INDENTATION_LEVEL = 4;

  /** Accumulates the partially built program. */
  private final StringBuilder buffer = new StringBuilder();

  private int indentation = 0;

  public PrintVisitor() {}

  /** Pretty print the given AST and return it as {@code String}. */
  public String run(ImpAstNode astNode) {
    // TODO: explain what happens if you call it multiple times. Or change the interface.
    astNode.accept(this);
    return buffer.toString();
  }

  /** Append current indentation to the buffer. */
  private void addIndentation() {
    for (int i = 0; i < this.indentation; i++) {
      buffer.append(' ');
    }
  }

  @Override
  public Void visit(LiteralNode literalNode) {
    buffer.append(literalNode.getValue());
    return null;
  }

  @Override
  public Void visit(ReadNode readNode) {
    buffer.append(readNode.getVariable());
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
  public Void visit(ArrayAccessNode arrAccessNode) {
    buffer.append(arrAccessNode.getVariable().toString());
    buffer.append("[");
    arrAccessNode.getIndex().accept(this);
    buffer.append("]");
    return null;
  }

  @Override
  public Void visit(DeclarationNode declarationNode) {
    addIndentation();

    buffer.append(declarationNode.getVariable());
    buffer.append(" : ");
    buffer.append(declarationNode.getLabel());

    buffer.append(';');
    return null;
  }

  @Override
  public Void visit(ArrayDeclarationNode declarationNode) {
    addIndentation();

    buffer.append(declarationNode.getVariable());

    buffer.append('[');
    buffer.append(declarationNode.getLength());
    buffer.append(']');

    buffer.append(" : ");
    buffer.append(declarationNode.getLabel().toString());

    buffer.append(';');
    return null;
  }

  @Override
  public Void visit(AssignNode assignNode) {
    addIndentation();

    buffer.append(assignNode.getVariable());
    buffer.append(" := ");
    assignNode.getRhs().accept(this);

    buffer.append(';');
    return null;
  }

  @Override
  public Void visit(SendNode sendNode) {
    addIndentation();

    buffer.append("send ");
    sendNode.getSentExpression().accept(this);
    buffer.append(" to ");
    buffer.append(sendNode.getRecipient());

    buffer.append(';');
    return null;
  }

  @Override
  public Void visit(ReceiveNode receiveNode) {
    // TODO: print annotation

    addIndentation();

    buffer.append(receiveNode.getVariable());
    buffer.append(" <- recv ");
    buffer.append(receiveNode.getSender());

    buffer.append(';');
    return null;
  }

  @Override
  public Void visit(AssertNode assertNode) {
    addIndentation();

    buffer.append("assert ");
    assertNode.getExpression().accept(this);
    buffer.append(';');
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
