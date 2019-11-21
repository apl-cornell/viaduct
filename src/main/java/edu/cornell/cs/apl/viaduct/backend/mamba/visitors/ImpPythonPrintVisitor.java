package edu.cornell.cs.apl.viaduct.backend.mamba.visitors;

import com.google.common.collect.ImmutableList;

import edu.cornell.cs.apl.viaduct.errors.ElaborationException;
import edu.cornell.cs.apl.viaduct.imp.ast.ArrayDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ArrayIndexingNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssertNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssignNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BinaryExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BinaryOperator;
import edu.cornell.cs.apl.viaduct.imp.ast.BinaryOperators;
import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BreakNode;
import edu.cornell.cs.apl.viaduct.imp.ast.DowngradeNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ForNode;
import edu.cornell.cs.apl.viaduct.imp.ast.IfNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LetBindingNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LiteralNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LoopNode;
import edu.cornell.cs.apl.viaduct.imp.ast.NotNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessName;
import edu.cornell.cs.apl.viaduct.imp.ast.ReadNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReceiveNode;
import edu.cornell.cs.apl.viaduct.imp.ast.SendNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StatementNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.imp.ast.VariableDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.WhileNode;
import edu.cornell.cs.apl.viaduct.imp.ast.types.BooleanType;
import edu.cornell.cs.apl.viaduct.imp.ast.types.ImpBaseType;
import edu.cornell.cs.apl.viaduct.imp.ast.types.IntegerType;
import edu.cornell.cs.apl.viaduct.imp.ast.values.BooleanValue;
import edu.cornell.cs.apl.viaduct.imp.ast.values.ImpValue;
import edu.cornell.cs.apl.viaduct.imp.ast.values.IntegerValue;
import edu.cornell.cs.apl.viaduct.imp.visitors.ExprVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.ReferenceVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;
import io.vavr.collection.Set;

import org.apache.commons.lang3.StringUtils;

/** convert imp programs to python programs. */
public final class ImpPythonPrintVisitor
    implements
        ReferenceVisitor<String>,
        ExprVisitor<String>,
        StmtVisitor<String>
{
  private static int INDENTATION_LEVEL = 2;

  private final ProcessName selfProcess;
  private final Set<ProcessName> mambaProcesses;

  private int indentation;

  public static String run(
      BlockNode block,
      ProcessName selfProcess,
      Set<ProcessName> mambaProcesses)
  {
    return block.accept(new ImpPythonPrintVisitor(selfProcess, mambaProcesses));
  }

  private ImpPythonPrintVisitor(ProcessName selfProcess, Set<ProcessName> mambaProcesses) {
    this.selfProcess = selfProcess;
    this.mambaProcesses = mambaProcesses;
    this.indentation = -INDENTATION_LEVEL;
  }

  private String addIndentation() {
    return StringUtils.repeat(' ', this.indentation);
  }

  private StringBuilder getBuilder() {
    StringBuilder builder = new StringBuilder();
    builder.append(addIndentation());
    return builder;
  }

  private String visitChildBlock(BlockNode node) {
    StringBuilder builder = new StringBuilder();
    builder.append('\n');
    this.indentation += INDENTATION_LEVEL;

    ImmutableList<StatementNode> stmts = node.getStatements();
    for (StatementNode stmt : stmts) {
      builder.append(stmt.accept(this));
      builder.append('\n');
    }

    if (stmts.size() == 0) {
      builder.append(addIndentation());
      builder.append("pass");
    }

    this.indentation -= INDENTATION_LEVEL;
    return builder.toString();
  }

  private String getDefaultValue(ImpBaseType type) {
    if (type instanceof BooleanType) {
      return "0";

    } else if (type instanceof IntegerType) {
      return "0";

    } else {
      // TODO: create new exception type
      throw new Error("unknown base type");
    }
  }

  @Override
  public String visit(Variable variable) {
    return variable.getName();
  }

  @Override
  public String visit(ArrayIndexingNode node) {
    return
        String.format(
            "%s[%s]",
            node.getArray().accept(this),
            node.getIndex().accept(this));
  }

  @Override
  public String visit(LiteralNode node) {
    ImpValue val = node.getValue();
    if (val instanceof BooleanValue) {
      return ((BooleanValue)val).getValue() ? "1" : "0";

    } else if (val instanceof IntegerValue) {
      return String.valueOf(((IntegerValue)val).getValue());

    } else {
      // TODO: add new exception type
      throw new Error("unknown ImpValue type");
    }
  }

  @Override
  public String visit(ReadNode node) {
    return node.getReference().accept(this);
  }

  @Override
  public String visit(NotNode node) {
    return String.format("(not %s)", node.accept(this));
  }

  @Override
  public String visit(BinaryExpressionNode node) {
    BinaryOperator op = node.getOperator();
    String opStr;

    if (op instanceof BinaryOperators.Or) {
      opStr = "or";

    } else if (op instanceof BinaryOperators.And) {
      opStr = "and";

    } else if (op instanceof BinaryOperators.EqualTo) {
      opStr = "==";

    } else if (op instanceof BinaryOperators.LessThan) {
      opStr = "<";

    } else if (op instanceof BinaryOperators.LessThanOrEqualTo) {
      opStr = "<=";

    } else if (op instanceof BinaryOperators.LessThan) {
      opStr = "<";

    } else if (op instanceof BinaryOperators.Plus) {
      opStr = "+";

    } else if (op instanceof BinaryOperators.Minus) {
      opStr = "-";

    } else if (op instanceof BinaryOperators.Times) {
      opStr = "*";

    } else if (op instanceof BinaryOperators.Divide) {
      opStr = "/";

    } else {
      // TODO: add actual exception
      throw new Error("unknown binary operator");
    }

    return
      String.format(
          "(%s %s %s)",
          node.getLhs().accept(this),
          opStr,
          node.getRhs().accept(this));
  }

  @Override
  public String visit(DowngradeNode node) {
    return node.getExpression().accept(this);
  }

  @Override
  public String visit(VariableDeclarationNode node) {
    return
        getBuilder()
        .append(
            String.format(
                "%s = %s",
                node.getVariable().accept(this),
                getDefaultValue(node.getType())))
        .toString();
  }

  @Override
  public String visit(ArrayDeclarationNode node) {
    return
        getBuilder()
        .append(
            String.format(
                "%s = [%s for _ in range(%s)]",
                node.getVariable().accept(this),
                getDefaultValue(node.getElementType()),
                node.getLength().accept(this)))
        .toString();
  }

  @Override
  public String visit(LetBindingNode node) {
    return
      getBuilder()
      .append(
          String.format(
              "%s = %s",
              node.getVariable().accept(this),
              node.getRhs().accept(this)))
      .toString();
  }

  @Override
  public String visit(AssignNode node) {
    return
        getBuilder()
        .append(
            String.format(
                "%s = %s",
                node.getLhs().accept(this),
                node.getRhs().accept(this)))
        .toString();
  }

  @Override
  public String visit(SendNode node) {
    ProcessName recipient = node.getRecipient();
    String template;

    if (this.selfProcess.equals(recipient)) {
      template = "user_output(%s)";

    } else if (this.mambaProcesses.contains(recipient)) {
      template = "mamba_input(%s)";

    } else {
      // TODO: new exception
      throw new Error("direct communication between hosts currently not supported");
    }

    return
        getBuilder()
        .append(String.format(template, node.getSentExpression().accept(this)))
        .toString();
  }

  @Override
  public String visit(ReceiveNode node) {
    ProcessName sender = node.getSender();
    String variableStr = node.getVariable().accept(this);

    if (this.selfProcess.equals(sender)) {
      return
          getBuilder()
          .append(
              String.format(
                  "%s = user_input(\"%s\", %s)",
                  variableStr, variableStr, variableStr))
          .toString();

    } else if (this.mambaProcesses.contains(sender)) {
      return
          getBuilder()
          .append(
              String.format("%s = mamba_output()", variableStr))
          .toString();

    } else {
      // TODO: new exception
      throw new Error("direct communication between hosts currently not supported");
    }
  }

  @Override
  public String visit(IfNode node) {
    return
      getBuilder()
      .append(String.format("if %s:%n", node.getGuard().accept(this)))
      .append(visitChildBlock(node.getThenBranch()))
      .append("\n")
      .append(addIndentation())
      .append("else:\n")
      .append(visitChildBlock(node.getElseBranch()))
      .toString();
  }

  @Override
  public String visit(WhileNode node) {
    return
      getBuilder()
      .append(String.format("while %s:%n", node.getGuard().accept(this)))
      .append(visitChildBlock(node.getBody()))
      .toString();
  }

  @Override
  public String visit(ForNode forNode) {
    throw new ElaborationException();
  }

  @Override
  public String visit(LoopNode node) {
    return
      getBuilder()
      .append("while True:\n")
      .append(visitChildBlock(node.getBody()))
      .toString();
  }

  @Override
  public String visit(BreakNode breakNode) {
    return getBuilder().append("break").toString();
  }

  @Override
  public String visit(BlockNode node) {
    return visitChildBlock(node);
  }

  @Override
  public String visit(AssertNode node) {
    return
        getBuilder()
        .append(String.format("assert %s", node.getExpression().accept(this)))
        .toString();
  }
}
