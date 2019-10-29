package edu.cornell.cs.apl.viaduct.backend.mamba.visitors;

import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaAssignNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaBinaryExpressionNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaBinaryOperator;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaBinaryOperators;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaBlockNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaExpressionNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaIfNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaInputNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaIntLiteralNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaOutputNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaReadNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaRegIntDeclarationNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaRegIntDeclarationNode.RegisterType;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaStatementNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaVariable;
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
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ForNode;
import edu.cornell.cs.apl.viaduct.imp.ast.IfNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LetBindingNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LiteralNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LoopNode;
import edu.cornell.cs.apl.viaduct.imp.ast.NotNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReadNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReceiveNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReferenceNode;
import edu.cornell.cs.apl.viaduct.imp.ast.SendNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StatementNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.imp.ast.VariableDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.WhileNode;
import edu.cornell.cs.apl.viaduct.imp.ast.values.BooleanValue;
import edu.cornell.cs.apl.viaduct.imp.ast.values.ImpValue;
import edu.cornell.cs.apl.viaduct.imp.ast.values.IntegerValue;
import edu.cornell.cs.apl.viaduct.imp.visitors.ExprVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.ReferenceVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;

import java.util.ArrayList;
import java.util.List;

public final class ImpToMambaTranslator
    implements
        ExprVisitor<MambaExpressionNode>,
        StmtVisitor<MambaStatementNode>,
        ReferenceVisitor<MambaVariable>
{
  private boolean isSecret;

  public static MambaStatementNode run(boolean isSecret, StatementNode stmt) {
    return stmt.accept(new ImpToMambaTranslator(isSecret));
  }

  public static MambaExpressionNode run(boolean isSecret, ExpressionNode expr) {
    return expr.accept(new ImpToMambaTranslator(isSecret));
  }

  public static MambaVariable run(boolean isSecret, ReferenceNode ref) {
    return ref.accept(new ImpToMambaTranslator(isSecret));
  }

  private ImpToMambaTranslator(boolean isSecret) {
    this.isSecret = isSecret;
  }

  @Override
  public MambaVariable visit(Variable var) {
    return MambaVariable.create(var.getName());
  }

  @Override
  public MambaVariable visit(ArrayIndexingNode arrayIndex) {
    throw new Error("translation not implemented");
  }

  @Override
  public MambaExpressionNode visit(LiteralNode node) {
    ImpValue value = node.getValue();

    // mamba has no native bool type, so convert to int
    int n;
    if (value instanceof BooleanValue) {
      BooleanValue boolValue = (BooleanValue) value;
      n = boolValue.getValue() ? 1 : 0;

    } else {
      IntegerValue intValue = (IntegerValue) value;
      n = intValue.getValue();
    }

    return MambaIntLiteralNode.create(n);
  }

  @Override
  public MambaExpressionNode visit(ReadNode node) {
    return MambaReadNode.create(node.getReference().accept(this));
  }

  @Override
  public MambaExpressionNode visit(NotNode node) {
    throw new Error("translation not implemented");
  }

  @Override
  public MambaExpressionNode visit(BinaryExpressionNode node) {
    MambaExpressionNode mambaLhs = node.getLhs().accept(this);
    MambaExpressionNode mambaRhs = node.getRhs().accept(this);

    BinaryOperator binOp = node.getOperator();
    MambaBinaryOperator mambaBinOp;
    if (binOp instanceof BinaryOperators.Or) {
      mambaBinOp = MambaBinaryOperators.Or.create();

    } else if (binOp instanceof BinaryOperators.And) {
      mambaBinOp = MambaBinaryOperators.And.create();

    } else if (binOp instanceof BinaryOperators.EqualTo) {
      mambaBinOp = MambaBinaryOperators.EqualTo.create();

    } else if (binOp instanceof BinaryOperators.LessThan) {
      mambaBinOp = MambaBinaryOperators.EqualTo.create();

    } else if (binOp instanceof BinaryOperators.LessThanOrEqualTo) {
      mambaBinOp = MambaBinaryOperators.LessThanOrEqualTo.create();

    } else if (binOp instanceof BinaryOperators.Plus) {
      mambaBinOp = MambaBinaryOperators.Plus.create();

    } else if (binOp instanceof BinaryOperators.Minus) {
      mambaBinOp = MambaBinaryOperators.Minus.create();

    } else if (binOp instanceof BinaryOperators.Times) {
      mambaBinOp = MambaBinaryOperators.Times.create();

    } else {
      throw new Error("translation of binary operator not implemented");
    }

    return
        MambaBinaryExpressionNode.builder()
        .setLhs(mambaLhs)
        .setRhs(mambaRhs)
        .setOperator(mambaBinOp)
        .build();
  }

  @Override
  public MambaExpressionNode visit(DowngradeNode node) {
    return node.getExpression().accept(this);
  }

  @Override
  public MambaStatementNode visit(VariableDeclarationNode node) {
    return
        MambaRegIntDeclarationNode.builder()
        .setRegisterType(this.isSecret ? RegisterType.SECRET : RegisterType.CLEAR)
        .setVariable(node.getVariable().accept(this))
        .build();
  }

  @Override
  public MambaStatementNode visit(ArrayDeclarationNode node) {
    throw new Error("translation not implemented");
  }

  @Override
  public MambaStatementNode visit(LetBindingNode node) {
    MambaExpressionNode mambaRhs = node.getRhs().accept(this);
    return
        MambaAssignNode.builder()
        .setVariable(node.getVariable().accept(this))
        .setRhs(mambaRhs)
        .build();
  }

  @Override
  public MambaStatementNode visit(AssignNode node) {
    MambaVariable mambaVar = node.getLhs().accept(this);
    MambaExpressionNode mambaRhs = node.getRhs().accept(this);
    return
        MambaAssignNode.builder()
        .setVariable(mambaVar)
        .setRhs(mambaRhs)
        .build();
  }

  @Override
  public MambaStatementNode visit(SendNode node) {
    return
        MambaOutputNode.builder()
        .setExpression(node.getSentExpression().accept(this))
        .setPlayer(0) // TODO: fix this
        .build();
  }

  @Override
  public MambaStatementNode visit(ReceiveNode node) {
    return
        MambaInputNode.builder()
        .setVariable(node.getVariable().accept(this))
        .setPlayer(0) // TODO: fix this
        .build();
  }

  @Override
  public MambaStatementNode visit(IfNode node) {
    return
      MambaIfNode.builder()
      .setGuard(node.getGuard().accept(this))
      .setThenBranch((MambaBlockNode) node.getThenBranch().accept(this))
      .setElseBranch((MambaBlockNode) node.getElseBranch().accept(this))
      .build();
  }

  @Override
  public MambaStatementNode visit(WhileNode node) {
    throw new Error("translation not implemented");
  }

  @Override
  public MambaStatementNode visit(ForNode node) {
    throw new Error("translation not implemented");
  }

  @Override
  public MambaStatementNode visit(LoopNode node) {
    throw new Error("translation not implemented");
  }

  @Override
  public MambaStatementNode visit(BreakNode node) {
    throw new Error("translation not implemented");
  }

  @Override
  public MambaStatementNode visit(BlockNode node) {
    List<MambaStatementNode> mambaStmts = new ArrayList<>();
    for (StatementNode stmt : node) {
      mambaStmts.add(stmt.accept(this));
    }
    return MambaBlockNode.builder().addAll(mambaStmts).build();
  }

  @Override
  public MambaStatementNode visit(AssertNode node) {
    throw new Error("translation not implemented");
  }
}
