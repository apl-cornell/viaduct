package edu.cornell.cs.apl.viaduct.backend.mamba.visitors;

import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaArrayDeclarationNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaArrayLoadNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaArrayStoreNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaAssignNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaBinaryExpressionNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaBinaryOperator;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaBinaryOperators;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaBlockNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaExpressionNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaIfNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaInputNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaIntLiteralNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaMuxNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaNegationNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaOutputNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaReadNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaRegIntDeclarationNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaRevealNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaSecurityType;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaStatementNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaVariable;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaWhileNode;
import edu.cornell.cs.apl.viaduct.util.FreshNameGenerator;
import io.vavr.collection.Set;

import org.apache.commons.lang3.StringUtils;

public final class MambaPrintVisitor
    implements MambaExpressionVisitor<String>, MambaStatementVisitor<String>
{
  private static int INDENTATION_LEVEL = 2;
  private static String THEN_BODY_NAME = "then_body";
  private static String ELSE_BODY_NAME = "else_body";
  private static String WHILE_BODY_NAME = "while_body";
  private static String OBJ_NAME = "obj";
  private static String TEMPLATE = OBJ_NAME + " = {}\n%s";

  private final FreshNameGenerator nameGenerator = new FreshNameGenerator();
  private final Set<MambaVariable> secretVariables;

  private int indentation = -INDENTATION_LEVEL;

  /** print. */
  public static String run(Set<MambaVariable> secretVariables, MambaStatementNode mambaProgram) {
    return
        String.format(TEMPLATE,
            mambaProgram.accept(new MambaPrintVisitor(secretVariables)));
  }

  private MambaPrintVisitor(Set<MambaVariable> secretVariables) {
    this.secretVariables = secretVariables;
  }

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

  private String visitControlBlock(
      String bodyName,
      String args,
      MambaBlockNode block,
      MambaExpressionNode returnExpr)
  {
    StringBuilder builder = new StringBuilder();
    builder.append('\n');
    builder.append(addIndentation());
    builder.append(String.format("def %s(%s):%n", bodyName, args));

    if (block.getStatements().size() > 0) {
      this.indentation += INDENTATION_LEVEL;
      builder.append(addIndentation());
      builder.append(String.format("global %s%n", OBJ_NAME));
      this.indentation -= INDENTATION_LEVEL;

      builder.append(visitChildBlock(block));

      if (returnExpr != null) {
        this.indentation += INDENTATION_LEVEL;
        builder.append(addIndentation());
        builder.append(String.format("return %s%n", returnExpr.accept(this)));
        this.indentation -= INDENTATION_LEVEL;
      }

    } else {
      this.indentation += INDENTATION_LEVEL;
      builder.append(addIndentation());
      builder.append("pass\n");
      this.indentation -= INDENTATION_LEVEL;
    }

    return builder.toString();
  }

  @Override
  public String visit(MambaIntLiteralNode node) {
    String intStr = String.valueOf(node.getValue());

    if (node.getSecurityType() == MambaSecurityType.CLEAR) {
      return String.format("regint(%s)", intStr);

    } else {
      return String.format("sregint(%s)", intStr);
    }
  }

  @Override
  public String visit(MambaReadNode node) {
    return String.format("%s.read()", visitVariable(node.getVariable()));
  }

  @Override
  public String visit(MambaArrayLoadNode node) {
    MambaVariable array = node.getArray();
    StringBuilder builder = new StringBuilder();

    if (this.secretVariables.contains(array)) {
      builder.append(
          String.format("secret_load(%s, %s)",
              visitVariable(array),
              node.getIndex().accept(this)));

    } else {
      builder.append(
          String.format("clear_load(%s, %s)",
              visitVariable(array),
              node.getIndex().accept(this)));
    }

    return builder.toString();
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
    return String.format("(1-%s)", node.getExpression().accept(this));
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

    return String.format("((%s * (%s - %s)) + %s)", guardStr, thenStr, elseStr, elseStr);
  }

  @Override
  public String visit(MambaRegIntDeclarationNode node) {
    StringBuilder builder = new StringBuilder();
    builder.append(addIndentation());

    switch (node.getRegisterType()) {
      case SECRET:
        builder.append(
            String.format("%s = MemValue(sregint())", visitVariable(node.getVariable())));
        break;

      case CLEAR:
      default:
        builder.append(
            String.format("%s = MemValue(regint())", visitVariable(node.getVariable())));
        break;
    }

    return builder.toString();
  }

  @Override
  public String visit(MambaArrayDeclarationNode node) {
    StringBuilder builder = new StringBuilder();
    builder.append(addIndentation());
    builder.append(
        String.format("%s = array_alloc(%s)",
            visitVariable(node.getVariable()),
            node.getLength().accept(this)));

    return builder.toString();
  }

  @Override
  public String visit(MambaAssignNode node) {
    MambaExpressionNode rhs = node.getRhs();

    StringBuilder builder = new StringBuilder();
    String template = null;

    boolean isBooleanExpr = IsBooleanExpr.run(rhs);
    boolean isSecretVariable = this.secretVariables.contains(node.getVariable());

    if (isBooleanExpr && isSecretVariable) {
      template = "%s.write(sregint(1) & %s)";

    } else if (isBooleanExpr && isSecretVariable) {
      template = "%s.write(regint(1) & %s)";

    } else {
      template = "%s.write(%s)";
    }

    builder.append(addIndentation());
    builder.append(
        String.format(template,
            visitVariable(node.getVariable()),
            node.getRhs().accept(this)));

    return builder.toString();
  }

  @Override
  public String visit(MambaArrayStoreNode node) {
    MambaVariable array = node.getArray();
    MambaExpressionNode value = node.getValue();

    StringBuilder builder = new StringBuilder();
    String template = null;

    boolean isBooleanExpr = IsBooleanExpr.run(value);
    boolean isSecretArray = this.secretVariables.contains(array);

    if (isBooleanExpr && isSecretArray) {
      template = "secret_store(%s, %s, sregint(1) & %s)";

    } else if (isBooleanExpr && !isSecretArray) {
      template = "clear_store(%s, %s, regint(1) & %s)";

    } else if (!isBooleanExpr && isSecretArray) {
      template = "secret_store(%s, %s, %s)";

    } else {
      template = "clear_store(%s, %s, %s)";
    }

    builder.append(addIndentation());
    builder.append(
        String.format(template,
            visitVariable(array),
            node.getIndex().accept(this),
            node.getValue().accept(this)));

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
      builder.append("MemValue(cint.get_input())");

    } else if (node.getSecurityContext() == MambaSecurityType.SECRET) {
      builder.append(
          String.format("MemValue(get_input(%d))", player));

    } else {
      builder.append(
          String.format("MemValue(get_input(%d).reveal())", player));
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
    builder.append(visitControlBlock(thenBodyName, "", node.getThenBranch(), null));

    String elseBodyName = this.nameGenerator.getFreshName(ELSE_BODY_NAME);
    builder.append(visitControlBlock(elseBodyName, "", node.getElseBranch(), null));

    builder.append('\n');
    String guardStr = node.getGuard().accept(this);
    builder.append(addIndentation());
    builder.append(String.format("if_statement(%s,%s,%s)", guardStr, thenBodyName, elseBodyName));

    return builder.toString();
  }

  @Override
  public String visit(MambaWhileNode node) {
    StringBuilder builder = new StringBuilder();

    String whileBodyName = nameGenerator.getFreshName(WHILE_BODY_NAME);
    builder.append(visitControlBlock(whileBodyName, "cond", node.getBody(), node.getGuard()));

    String guardStr = node.getGuard().accept(this);

    builder.append('\n');
    builder.append(addIndentation());
    builder.append(String.format("do_loop(%s, %s)", guardStr, whileBodyName));

    return builder.toString();
  }

  @Override
  public String visit(MambaBlockNode node) {
    StringBuilder builder = new StringBuilder();
    builder.append(addIndentation());
    builder.append(visitChildBlock(node));

    return builder.toString();
  }

  private static class IsBooleanExpr implements MambaExpressionVisitor<Boolean> {
    private IsBooleanExpr() {}

    public static Boolean run(MambaExpressionNode expr) {
      return expr.accept(new IsBooleanExpr());
    }

    @Override
    public Boolean visit(MambaIntLiteralNode node) {
      return false;
    }

    @Override
    public Boolean visit(MambaReadNode node) {
      return false;
    }

    @Override
    public Boolean visit(MambaArrayLoadNode node) {
      return false;
    }

    @Override
    public Boolean visit(MambaBinaryExpressionNode node) {
      MambaBinaryOperator binOp = node.getOperator();
      if (binOp instanceof MambaBinaryOperators.Or) {
        return true;

      } else if (binOp instanceof MambaBinaryOperators.And) {
        return true;

      } else if (binOp instanceof MambaBinaryOperators.EqualTo) {
        return true;

      } else if (binOp instanceof MambaBinaryOperators.LessThan) {
        return true;

      } else if (binOp instanceof MambaBinaryOperators.LessThanOrEqualTo) {
        return true;

      } else if (binOp instanceof MambaBinaryOperators.Plus) {
        return false;

      } else if (binOp instanceof MambaBinaryOperators.Minus) {
        return false;

      } else if (binOp instanceof MambaBinaryOperators.Times) {
        return false;

      } else {
        throw new Error("unknown MAMBA binary operator");
      }
    }

    @Override
    public Boolean visit(MambaNegationNode node) {
      return true;
    }

    @Override
    public Boolean visit(MambaRevealNode node) {
      return false;
    }

    @Override
    public Boolean visit(MambaMuxNode node) {
      return false;
    }
  }
}

