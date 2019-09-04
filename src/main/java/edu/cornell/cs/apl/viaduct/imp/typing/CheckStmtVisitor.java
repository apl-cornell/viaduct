package edu.cornell.cs.apl.viaduct.imp.typing;

import edu.cornell.cs.apl.viaduct.errors.TypeMismatchError;
import edu.cornell.cs.apl.viaduct.imp.ast.ArrayDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ArrayIndexingNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ArrayType;
import edu.cornell.cs.apl.viaduct.imp.ast.AssertNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssignNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BinaryExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BinaryOperatorType;
import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BooleanType;
import edu.cornell.cs.apl.viaduct.imp.ast.BreakNode;
import edu.cornell.cs.apl.viaduct.imp.ast.DowngradeNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ForNode;
import edu.cornell.cs.apl.viaduct.imp.ast.IfNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpBaseType;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpType;
import edu.cornell.cs.apl.viaduct.imp.ast.IntegerType;
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
import edu.cornell.cs.apl.viaduct.imp.parser.Located;
import edu.cornell.cs.apl.viaduct.imp.visitors.AbstractExprVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.AbstractReferenceVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.ContextStmtVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.ExprVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.ReferenceVisitor;

final class CheckStmtVisitor
    extends ContextStmtVisitor<CheckStmtVisitor, ImpType, ImpType, ImpBaseType, Void> {
  private final CheckReferenceVisitor referenceVisitor = new CheckReferenceVisitor();
  private final CheckExprVisitor expressionVisitor = new CheckExprVisitor();
  private final int loopLevel;

  CheckStmtVisitor() {
    this.loopLevel = 0;
  }

  /** Create a new visitor where the context is copied from the given visitor. */
  private CheckStmtVisitor(
      ContextStmtVisitor<CheckStmtVisitor, ImpType, ImpType, ImpBaseType, Void> contextStmtVisitor,
      int loopLevel) {
    super(contextStmtVisitor);
    this.loopLevel = loopLevel;
  }

  @Override
  protected ReferenceVisitor<ImpType> getReferenceVisitor() {
    return referenceVisitor;
  }

  @Override
  protected ExprVisitor<ImpBaseType> getExpressionVisitor() {
    return expressionVisitor;
  }

  /** Assert that a node has the given type. */
  private void assertHasType(Located node, ImpType actualType, ImpType expectedType) {
    if (!actualType.equals(expectedType)) {
      throw new TypeMismatchError(node, actualType, expectedType);
    }
  }

  @Override
  protected ImpType extract(VariableDeclarationNode node) {
    return node.getType();
  }

  @Override
  protected ImpType extract(ArrayDeclarationNode node, ImpBaseType lengthType) {
    assertHasType(node.getLength(), lengthType, IntegerType.create());
    return ArrayType.create(node.getElementType());
  }

  @Override
  protected ImpType extract(LetBindingNode node, ImpBaseType rhsType) {
    return rhsType;
  }

  @Override
  protected CheckStmtVisitor newScope() {
    return new CheckStmtVisitor(this, loopLevel);
  }

  @Override
  protected CheckStmtVisitor enter(StatementNode node) {
    return this;
  }

  @Override
  protected Void leave(VariableDeclarationNode node, CheckStmtVisitor visitor) {
    return null;
  }

  @Override
  protected Void leave(
      ArrayDeclarationNode node, CheckStmtVisitor visitor, ImpBaseType lengthType) {
    return null;
  }

  @Override
  protected Void leave(LetBindingNode node, CheckStmtVisitor visitor, ImpBaseType rhsType) {
    return null;
  }

  @Override
  protected Void leave(
      AssignNode node, CheckStmtVisitor visitor, ImpType lhsType, ImpBaseType rhsType) {
    assertHasType(node.getRhs(), rhsType, lhsType);
    return null;
  }

  @Override
  protected Void leave(SendNode node, CheckStmtVisitor visitor, ImpBaseType sentExpression) {
    // TODO: add session types so we can check we are sending the correct type of value.
    return null;
  }

  @Override
  protected Void leave(ReceiveNode node, CheckStmtVisitor visitor, ImpType lhsType) {
    // TODO: add session types so we can check we are sending the correct type of value.
    if (node.getReceiveType() != null) {
      assertHasType(node, node.getReceiveType(), lhsType);
    }
    return null;
  }

  @Override
  protected Void leave(
      IfNode node,
      CheckStmtVisitor visitor,
      ImpBaseType guardType,
      Void thenBranch,
      Void elseBranch) {
    assertHasType(node.getGuard(), guardType, BooleanType.create());
    return null;
  }

  @Override
  protected Void leave(WhileNode node, CheckStmtVisitor visitor, ImpBaseType guardType, Void body) {
    assertHasType(node.getGuard(), guardType, BooleanType.create());
    return null;
  }

  @Override
  protected Void leave(
      ForNode node,
      CheckStmtVisitor visitor,
      Void initialize,
      ImpBaseType guardType,
      Void update,
      Void body) {
    assertHasType(node.getGuard(), guardType, BooleanType.create());
    return null;
  }

  @Override
  protected Void leave(LoopNode node, CheckStmtVisitor visitor, Void body) {
    // TODO: increase loop counter for the body
    return null;
  }

  @Override
  protected Void leave(BreakNode node, CheckStmtVisitor visitor) {
    return null;
  }

  @Override
  protected Void leave(BlockNode node, CheckStmtVisitor visitor, Iterable<Void> statements) {
    return null;
  }

  @Override
  protected Void leave(AssertNode node, CheckStmtVisitor visitor, ImpBaseType expressionType) {
    assertHasType(node.getExpression(), expressionType, BooleanType.create());
    return null;
  }

  private final class CheckReferenceVisitor
      extends AbstractReferenceVisitor<CheckReferenceVisitor, ImpType, ImpBaseType> {

    @Override
    protected ExprVisitor<ImpBaseType> getExpressionVisitor() {
      return CheckStmtVisitor.this.getExpressionVisitor();
    }

    @Override
    protected CheckReferenceVisitor enter(ReferenceNode node) {
      return this;
    }

    @Override
    protected ImpType leave(Variable node, CheckReferenceVisitor visitor) {
      return CheckStmtVisitor.this.get(node);
    }

    @Override
    protected ImpType leave(
        ArrayIndexingNode node,
        CheckReferenceVisitor visitor,
        ImpType arrayType,
        ImpBaseType indexType) {
      if (!(arrayType instanceof ArrayType)) {
        throw new TypeMismatchError(node.getArray(), arrayType, RawArrayType.create());
      }
      assertHasType(node.getIndex(), indexType, IntegerType.create());
      return ((ArrayType) arrayType).getElementType();
    }
  }

  private final class CheckExprVisitor
      extends AbstractExprVisitor<CheckExprVisitor, ImpType, ImpBaseType> {

    @Override
    protected ReferenceVisitor<ImpType> getReferenceVisitor() {
      return CheckStmtVisitor.this.getReferenceVisitor();
    }

    @Override
    protected CheckExprVisitor enter(ExpressionNode node) {
      return this;
    }

    @Override
    protected ImpBaseType leave(LiteralNode node, CheckExprVisitor visitor) {
      return node.getValue().getType();
    }

    @Override
    protected ImpBaseType leave(ReadNode node, CheckExprVisitor visitor, ImpType referenceType) {
      // TODO: better error message
      return (ImpBaseType) referenceType;
    }

    @Override
    protected ImpBaseType leave(
        NotNode node, CheckExprVisitor visitor, ImpBaseType expressionType) {
      assertHasType(node, expressionType, BooleanType.create());
      return BooleanType.create();
    }

    @Override
    protected ImpBaseType leave(
        BinaryExpressionNode node,
        CheckExprVisitor visitor,
        ImpBaseType lhsType,
        ImpBaseType rhsType) {
      final BinaryOperatorType operatorType = node.getOperator().getType();
      assertHasType(node.getLhs(), lhsType, operatorType.getLhsType());
      assertHasType(node.getRhs(), rhsType, operatorType.getRhsType());
      return operatorType.getReturnType();
    }

    @Override
    protected ImpBaseType leave(
        DowngradeNode node, CheckExprVisitor visitor, ImpBaseType expressionType) {
      return expressionType;
    }
  }
}
