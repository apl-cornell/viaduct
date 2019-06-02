package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.imp.ast.AndNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ArrayDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssignNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BinaryExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.DeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.DowngradeNode;
import edu.cornell.cs.apl.viaduct.imp.ast.EqualToNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Host;
import edu.cornell.cs.apl.viaduct.imp.ast.IfNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LeqNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LessThanNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LiteralNode;
import edu.cornell.cs.apl.viaduct.imp.ast.NotNode;
import edu.cornell.cs.apl.viaduct.imp.ast.OrNode;
import edu.cornell.cs.apl.viaduct.imp.ast.PlusNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessConfigurationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReadNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReceiveNode;
import edu.cornell.cs.apl.viaduct.imp.ast.SendNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;
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

    // Don't print curly braces around the top level program
    if (astNode instanceof BlockNode) {
      for (StmtNode stmt : (BlockNode) astNode) {
        stmt.accept(this);
        buffer.append('\n');
      }
    } else {
      astNode.accept(this);
    }

    return buffer.toString();
  }

  /** Append current indentation to the buffer. */
  private void addIndentation() {
    for (int i = 0; i < this.indentation; i++) {
      buffer.append(' ');
    }
  }

  private Void visitBinary(BinaryExpressionNode binaryExpressionNode, String op) {
    buffer.append('(');
    binaryExpressionNode.getLhs().accept(this);

    buffer.append(' ');
    buffer.append(op);
    buffer.append(' ');

    binaryExpressionNode.getRhs().accept(this);
    buffer.append(')');

    return null;
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
  public Void visit(OrNode orNode) {
    return visitBinary(orNode, "||");
  }

  @Override
  public Void visit(AndNode andNode) {
    return visitBinary(andNode, "&&");
  }

  @Override
  public Void visit(EqualToNode equalToNode) {
    return visitBinary(equalToNode, "==");
  }

  @Override
  public Void visit(LessThanNode lessThanNode) {
    return visitBinary(lessThanNode, "<");
  }

  @Override
  public Void visit(LeqNode leqNode) {
    return visitBinary(leqNode, "<=");
  }

  @Override
  public Void visit(PlusNode plusNode) {
    return visitBinary(plusNode, "+");
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
    buffer.append(" <- ");
    buffer.append(receiveNode.getSender());

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
  public Void visit(ProcessConfigurationNode processConfigurationNode) {
    boolean first = true;
    for (Tuple2<Host, StmtNode> process : processConfigurationNode) {
      if (!first) {
        buffer.append("\n\n");
      }

      final Host host = process._1();
      final StmtNode statement = process._2();
      buffer.append(host);
      buffer.append(' ');
      statement.accept(this);

      first = false;
    }

    return null;
  }
}
