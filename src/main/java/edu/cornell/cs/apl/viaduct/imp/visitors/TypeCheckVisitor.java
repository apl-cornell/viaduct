package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.imp.RedeclaredVariableException;
import edu.cornell.cs.apl.viaduct.imp.TypeCheckException;
import edu.cornell.cs.apl.viaduct.imp.UndeclaredVariableException;
import edu.cornell.cs.apl.viaduct.imp.ast.ArrayDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ArrayIndex;
import edu.cornell.cs.apl.viaduct.imp.ast.AssertNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssignNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BinaryExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BooleanType;
import edu.cornell.cs.apl.viaduct.imp.ast.BreakNode;
import edu.cornell.cs.apl.viaduct.imp.ast.DowngradeNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ForNode;
import edu.cornell.cs.apl.viaduct.imp.ast.IfNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpType;
import edu.cornell.cs.apl.viaduct.imp.ast.IntegerType;
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
import edu.cornell.cs.apl.viaduct.util.SymbolTable;
import io.vavr.Tuple2;

/**
 * Check that value types are cohesive (e.g. only integers are added together).
 * Label checking is a separate step.
 */
public class TypeCheckVisitor
    implements ReferenceVisitor<ImpType>,
        ExprVisitor<ImpType>,
        StmtVisitor<Void>,
        ProgramVisitor<Void> {

  private final SymbolTable<Variable, ImpType> symbolTable;
  private final SymbolTable<Variable, ImpType> tempSymbolTable;
  private int loopLevel;

  /** constructor. */
  public TypeCheckVisitor() {
    this.symbolTable = new SymbolTable<>();
    this.tempSymbolTable = new SymbolTable<>();
    this.loopLevel = 0;
  }

  public void run(ExpressionNode expr) {
    this.loopLevel = 0;
    expr.accept(this);
  }

  public void run(StmtNode stmt) {
    this.loopLevel = 0;
    stmt.accept(this);
  }

  public void run(ProgramNode program) {
    this.loopLevel = 0;
    program.accept(this);
  }

  /** Assert that an expression has the given type. */
  private void assertHasType(ExpressionNode expression, ImpType expectedType) {
    ImpType actualType = expression.accept(this);

    if (!actualType.equals(expectedType)) {
      throw new TypeCheckException(expression, expectedType, actualType);
    }
  }

  @Override
  public ImpType visit(Variable variable) {
    if (this.symbolTable.contains(variable)) {
      return this.symbolTable.get(variable);
    } else {
      throw new UndeclaredVariableException(variable);
    }
  }

  @Override
  public ImpType visit(ArrayIndex arrayIndex) {
    Variable var = arrayIndex.getArray();
    ExpressionNode index = arrayIndex.getIndex();
    ImpType indexType = index.accept(this);

    if (indexType instanceof IntegerType) {
      return this.visit(var);
    } else {
      throw new TypeCheckException(
          String.format("Index %s of array %s not an integer", index, var));
    }
  }

  @Override
  public ImpType visit(ReadNode readNode) {
    return readNode.getReference().accept(this);
  }

  @Override
  public ImpType visit(LiteralNode literalNode) {
    return literalNode.getValue().getType();
  }

  @Override
  public ImpType visit(NotNode notNode) {
    assertHasType(notNode.getExpression(), BooleanType.create());
    return BooleanType.create();
  }

  @Override
  public ImpType visit(BinaryExpressionNode binaryExpressionNode) {
    ImpType lhsType = binaryExpressionNode.getLhs().accept(this);
    ImpType rhsType = binaryExpressionNode.getRhs().accept(this);
    return binaryExpressionNode.getOperator().typeCheck(lhsType, rhsType);
  }

  @Override
  public ImpType visit(DowngradeNode downgradeNode) {
    return downgradeNode.getExpression().accept(this);
  }

  @Override
  public Void visit(VariableDeclarationNode varDecl) {
    if (this.symbolTable.contains(varDecl.getVariable())) {
      throw new RedeclaredVariableException(varDecl.getVariable());
    }
    this.symbolTable.add(varDecl.getVariable(), varDecl.getType());
    return null;
  }

  @Override
  public Void visit(ArrayDeclarationNode arrDeclNode) {
    assertHasType(arrDeclNode.getLength(), IntegerType.create());

    if (this.symbolTable.contains(arrDeclNode.getVariable())) {
      throw new RedeclaredVariableException(arrDeclNode.getVariable());
    }
    // TODO: this should be a separate table, or we should have array types.
    this.symbolTable.add(arrDeclNode.getVariable(), arrDeclNode.getType());
    return null;
  }

  @Override
  public Void visit(LetBindingNode letBindingNode) {
    ImpType rhsType = letBindingNode.getRhs().accept(this);
    this.tempSymbolTable.add(letBindingNode.getVariable(), rhsType);
    return null;
  }

  @Override
  public Void visit(AssignNode assignNode) {
    ImpType lhsType = assignNode.getLhs().accept(this);
    assertHasType(assignNode.getRhs(), lhsType);
    return null;
  }

  @Override
  public Void visit(SendNode sendNode) {
    // you need session types to type check this properly...
    // OTOH surface programs shouldn't really even have send and recvs

    // at least check that the expression we're sending is consistent (typechecks)
    sendNode.getSentExpression().accept(this);
    return null;
  }

  @Override
  public Void visit(ReceiveNode recvNode) {
    // you need session types to type check this properly...
    // OTOH surface programs shouldn't really even have send and recvs

    // receives are either a binding form (like let) or assignment to a declared variable
    Variable recvVar = recvNode.getVariable();
    ImpType recvType = recvNode.getRecvType();
    if (!this.symbolTable.contains(recvVar)) { // binding to a temporary
      this.tempSymbolTable.add(recvVar, recvType);
    }
    return null;
  }

  @Override
  public Void visit(IfNode ifNode) {
    assertHasType(ifNode.getGuard(), BooleanType.create());
    ifNode.getThenBranch().accept(this);
    ifNode.getElseBranch().accept(this);

    return null;
  }

  @Override
  public Void visit(WhileNode whileNode) {
    assertHasType(whileNode.getGuard(), BooleanType.create());
    this.symbolTable.push();
    whileNode.getBody().accept(this);
    this.symbolTable.pop();

    return null;
  }

  @Override
  public Void visit(ForNode forNode) {
    // TODO: check that this is the right way to scope this.
    this.symbolTable.push();

    assertHasType(forNode.getGuard(), BooleanType.create());
    forNode.getInitialize().accept(this);
    forNode.getUpdate().accept(this);
    forNode.getBody().accept(this);

    this.symbolTable.pop();
    return null;
  }

  @Override
  public Void visit(LoopNode loopNode) {
    this.symbolTable.push();
    this.loopLevel++;
    loopNode.getBody().accept(this);
    this.loopLevel--;
    this.symbolTable.pop();

    return null;
  }

  @Override
  public Void visit(BreakNode breakNode) {
    if (this.loopLevel > 0) {
      assertHasType(breakNode.getLevel(), IntegerType.create());
      return null;

    } else {
      throw new TypeCheckException("Break is outside of loop");
    }
  }

  @Override
  public Void visit(BlockNode blockNode) {
    this.symbolTable.push();
    this.tempSymbolTable.push();

    for (StmtNode stmt : blockNode) {
      stmt.accept(this);
    }

    this.symbolTable.pop();
    this.tempSymbolTable.pop();

    return null;
  }

  @Override
  public Void visit(AssertNode assertNode) {
    assertHasType(assertNode.getExpression(), BooleanType.create());
    return null;
  }

  @Override
  public Void visit(ProgramNode programNode) {
    for (Tuple2<ProcessName, StmtNode> process : programNode) {
      process._2().accept(this);
    }
    return null;
  }
}
