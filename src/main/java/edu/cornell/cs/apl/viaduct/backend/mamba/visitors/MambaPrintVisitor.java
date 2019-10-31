package edu.cornell.cs.apl.viaduct.backend.mamba.visitors;

import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaAssignNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaBinaryExpressionNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaBlockNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaIfNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaInputNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaIntLiteralNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaMuxNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaNegationNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaOutputNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaReadNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaRegIntDeclarationNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaRevealNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaStatementNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaVariable;
import edu.cornell.cs.apl.viaduct.util.FreshNameGenerator;

import org.apache.commons.lang3.StringUtils;

public final class MambaPrintVisitor
    implements MambaExpressionVisitor<String>, MambaStatementVisitor<String>
{
  private static int INDENTATION_LEVEL = 2;
  private static String THEN_BODY_NAME = "then_body";
  private static String ELSE_BODY_NAME = "else_body";
  private static String OBJ_NAME = "obj";
  private static String WRAPPER_NAME = "wrapper";
  private static String TEMPLATE = OBJ_NAME + " = {}\n%s";

  private int indentation = -INDENTATION_LEVEL;
  private FreshNameGenerator nameGenerator = new FreshNameGenerator();

  public static String run(MambaStatementNode mambaProgram) {
    return String.format(TEMPLATE, mambaProgram.accept(new MambaPrintVisitor()));
  }

  private MambaPrintVisitor() {}

  private String addIndentation() {
    return StringUtils.repeat(' ', this.indentation);
  }

  private String visitChildBlock(MambaBlockNode node) {
    StringBuilder builder = new StringBuilder();
    builder.append('\n');
    this.indentation += INDENTATION_LEVEL;

    for (MambaStatementNode stmt : node) {
      builder.append(stmt.accept(this));
      builder.append('\n');
    }

    this.indentation -= INDENTATION_LEVEL;
    return builder.toString();
  }

  private String visitVariable(MambaVariable var) {
    return String.format("%s[\"%s\"]", OBJ_NAME, var.getName());
  }

  private String visitConditionalBranch(String bodyName, MambaBlockNode branch) {
    StringBuilder builder = new StringBuilder();
    builder.append('\n');
    builder.append(addIndentation());
    builder.append(String.format("def %s(%s):\n", bodyName, OBJ_NAME));

    this.indentation += INDENTATION_LEVEL;
    builder.append(addIndentation());
    builder.append(String.format("def %s():", WRAPPER_NAME));

    if (branch.getStatements().size() > 0) {
      builder.append(visitChildBlock(branch));

    } else {
      this.indentation += INDENTATION_LEVEL;
      builder.append(addIndentation());
      builder.append("pass\n");
      this.indentation -= INDENTATION_LEVEL;
    }

    builder.append('\n');
    builder.append(addIndentation());
    builder.append(String.format("return %s\n", WRAPPER_NAME));
    this.indentation -= INDENTATION_LEVEL;

    return builder.toString();
  }

  @Override
  public String visit(MambaIntLiteralNode node) {
    return String.format("%s", String.valueOf(node.getValue()));
  }

  @Override
  public String visit(MambaReadNode node) {
    return visitVariable(node.getVariable());
  }

  @Override
  public String visit(MambaBinaryExpressionNode node) {
    StringBuilder builder = new StringBuilder();
    builder.append("(");
    builder.append(node.getLhs().accept(this));
    builder.append(" ");
    builder.append(node.getOperator().toString());
    builder.append(" ");
    builder.append(node.getRhs().accept(this));
    builder.append(")");
    return builder.toString();
  }

  @Override
  public String visit(MambaNegationNode node) {
    return String.format("((1-(%s)) > 0)", node.getExpression().accept(this));
  }

  @Override
  public String visit(MambaRevealNode node) {
    String exprStr = node.getRevealedExpr().accept(this);
    return String.format("(%s).reveal()", exprStr);
  }

  @Override
  public String visit(MambaMuxNode node) {
    String guardStr = node.getGuard().accept(this);
    String thenStr = node.getThenValue().accept(this);
    String elseStr = node.getElseValue().accept(this);

    return String.format("(%s + ((%s > 0) * (%s - %s)))", elseStr, guardStr, thenStr, elseStr);
  }

  @Override
  public String visit(MambaRegIntDeclarationNode node) {
    StringBuilder builder = new StringBuilder();
    builder.append(addIndentation());

    switch (node.getRegisterType()) {
      case SECRET:
        builder.append(String.format("%s = sint()", visitVariable(node.getVariable())));
        break;

      case CLEAR:
      default:
        builder.append(String.format("%s = cint()", visitVariable(node.getVariable())));
        break;
    }

    return builder.toString();
  }

  @Override
  public String visit(MambaAssignNode node) {
    StringBuilder builder = new StringBuilder();
    builder.append(addIndentation());
    builder.append(visitVariable(node.getVariable()));
    builder.append(" = ");
    builder.append(node.getRhs().accept(this));
    return builder.toString();
  }

  @Override
  public String visit(MambaInputNode node) {
    StringBuilder builder = new StringBuilder();
    builder.append(addIndentation());
    builder.append(visitVariable(node.getVariable()));
    builder.append(" = ");

    int player = node.getPlayer();
    // if player < 0, then it is public input
    if (player < 0) {
      builder.append("cint.get_input()");

    } else {
      builder.append(String.format("sint.get_private_input_from(%d)", player));
    }

    return builder.toString();
  }

  @Override
  public String visit(MambaOutputNode node) {
    StringBuilder builder = new StringBuilder();
    builder.append(addIndentation());

    String exprStr = node.getExpression().accept(this);

    int player = node.getPlayer();
    if (player < 0) {
      builder.append(String.format("(%s).public_output()", exprStr));

    } else {
      builder.append(String.format("(%s).reveal_to(%d)", exprStr, player));

    }

    return builder.toString();
  }

  @Override
  public String visit(MambaIfNode node) {
    StringBuilder builder = new StringBuilder();

    String thenBodyName = this.nameGenerator.getFreshName(THEN_BODY_NAME);
    builder.append(visitConditionalBranch(thenBodyName, node.getThenBranch()));

    String elseBodyName = this.nameGenerator.getFreshName(ELSE_BODY_NAME);
    builder.append(visitConditionalBranch(elseBodyName, node.getElseBranch()));

    builder.append('\n');
    String guardStr = node.getGuard().accept(this);
    builder.append(addIndentation());
    builder.append(
        String.format("if_statement(%s,%s(%s),%s(%s))",
            guardStr, thenBodyName, OBJ_NAME, elseBodyName, OBJ_NAME));

    return builder.toString();
  }

  @Override
  public String visit(MambaBlockNode node) {
    StringBuilder builder = new StringBuilder();
    builder.append(addIndentation());
    builder.append(visitChildBlock(node));

    return builder.toString();
  }
}

