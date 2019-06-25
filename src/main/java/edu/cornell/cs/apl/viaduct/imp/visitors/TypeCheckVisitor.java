package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.imp.TypeCheckException;
import edu.cornell.cs.apl.viaduct.imp.ast.AbstractArrayAccessNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ArrayAccessNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ArrayDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ArrayIndexNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssertNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssignNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BinaryExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BoolType;
import edu.cornell.cs.apl.viaduct.imp.ast.BooleanValue;
import edu.cornell.cs.apl.viaduct.imp.ast.DeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.DowngradeNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ForNode;
import edu.cornell.cs.apl.viaduct.imp.ast.IfNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpType;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpValue;
import edu.cornell.cs.apl.viaduct.imp.ast.IntType;
import edu.cornell.cs.apl.viaduct.imp.ast.IntegerValue;
import edu.cornell.cs.apl.viaduct.imp.ast.LExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LReadNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LiteralNode;
import edu.cornell.cs.apl.viaduct.imp.ast.NotNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessName;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReadNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReceiveNode;
import edu.cornell.cs.apl.viaduct.imp.ast.SendNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.imp.ast.WhileNode;
import edu.cornell.cs.apl.viaduct.util.SymbolTable;
import io.vavr.Tuple2;

/** type-check an AST. */
public class TypeCheckVisitor
    implements ExprVisitor<ImpType>,
        StmtVisitor<Void>,
        LExprVisitor<ImpType>,
        ProgramVisitor<Void> {

  private SymbolTable<Variable, ImpType> symbolTable;

  public TypeCheckVisitor() {
    this.symbolTable = new SymbolTable<>();
  }

  public void run(ExpressionNode expr) {
    expr.accept(this);
  }

  public void run(StmtNode stmt) {
    stmt.accept(this);
  }

  public void run(ProgramNode program) {
    program.accept(this);
  }

  protected ImpType visitVariable(Variable var) {
    if (this.symbolTable.contains(var)) {
      return this.symbolTable.get(var);

    } else {
      throw new Error(new TypeCheckException(var));
    }
  }

  protected ImpType visitArrayAccess(AbstractArrayAccessNode arrAccessNode) {
    Variable var = arrAccessNode.getVariable();
    ExpressionNode index = arrAccessNode.getIndex();
    ImpType indexType = index.accept(this);

    if (indexType instanceof IntType) {
      return visitVariable(var);

    } else {
      throw new Error(
          new TypeCheckException(String.format("Index %s of array %s not an integer", index, var)));
    }
  }

  protected ImpType visitGuard(StmtNode control, ExpressionNode guard) {
    ImpType guardType = guard.accept(this);

    if (!(guardType instanceof BoolType)) {
      throw new Error(
          new TypeCheckException(
              String.format("Guard of %s is not boolean, has type %s", control, guardType)));

    } else {
      return BoolType.instance();
    }
  }

  @Override
  public ImpType visit(ReadNode readNode) {
    return visitVariable(readNode.getVariable());
  }

  @Override
  public ImpType visit(LiteralNode literalNode) {
    ImpValue val = literalNode.getValue();
    if (val instanceof BooleanValue) {
      return BoolType.instance();

    } else if (val instanceof IntegerValue) {
      return IntType.instance();

    } else {
      throw new Error(new TypeCheckException("unknown value " + val));
    }
  }

  @Override
  public ImpType visit(NotNode notNode) {
    ExpressionNode expr = notNode.getExpression();
    ImpType exprType = expr.accept(this);

    if (exprType instanceof BoolType) {
      return BoolType.instance();

    } else {
      throw new Error(new TypeCheckException(expr, exprType, BoolType.instance()));
    }
  }

  @Override
  public ImpType visit(BinaryExpressionNode binaryExpressionNode) {
    try {
      ImpType lhsType = binaryExpressionNode.getLhs().accept(this);
      ImpType rhsType = binaryExpressionNode.getRhs().accept(this);
      return binaryExpressionNode.getOperator().typeCheck(lhsType, rhsType);

    } catch (TypeCheckException tcException) {
      throw new Error(tcException);
    }
  }

  @Override
  public ImpType visit(DowngradeNode downgradeNode) {
    return downgradeNode.getExpression().accept(this);
  }

  @Override
  public ImpType visit(ArrayAccessNode arrAccessNode) {
    return visitArrayAccess(arrAccessNode);
  }

  @Override
  public ImpType visit(ArrayIndexNode arrIndexNode) {
    return visitArrayAccess(arrIndexNode);
  }

  @Override
  public ImpType visit(LReadNode lreadNode) {
    return visitVariable(lreadNode.getVariable());
  }

  @Override
  public Void visit(DeclarationNode varDecl) {
    this.symbolTable.add(varDecl.getVariable(), varDecl.getType());
    return null;
  }

  @Override
  public Void visit(ArrayDeclarationNode arrDeclNode) {
    ExpressionNode lengthExpr = arrDeclNode.getLength();
    ImpType lengthType = lengthExpr.accept(this);

    if (!(lengthType instanceof IntType)) {
      throw new Error(
          new TypeCheckException(
              String.format("Length %s of array %s not an integer", lengthExpr, arrDeclNode)));
    }

    this.symbolTable.add(arrDeclNode.getVariable(), arrDeclNode.getType());
    return null;
  }

  @Override
  public Void visit(AssignNode assignNode) {
    LExpressionNode lhs = assignNode.getLhs();
    ExpressionNode rhs = assignNode.getRhs();
    ImpType lhsType = lhs.accept(this);
    ImpType rhsType = rhs.accept(this);

    if (!lhsType.equals(rhsType)) {
      throw new Error(new TypeCheckException(lhs, lhsType, rhs, rhsType));
    }

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
  public Void visit(ReceiveNode receiveNode) {
    // you need session types to type check this properly...
    // OTOH surface programs shouldn't really even have send and recvs
    return null;
  }

  @Override
  public Void visit(IfNode ifNode) {
    visitGuard(ifNode, ifNode.getGuard());
    ifNode.getThenBranch().accept(this);
    ifNode.getElseBranch().accept(this);

    return null;
  }

  @Override
  public Void visit(WhileNode whileNode) {
    visitGuard(whileNode, whileNode.getGuard());
    whileNode.getBody().accept(this);

    return null;
  }

  @Override
  public Void visit(ForNode forNode) {
    visitGuard(forNode, forNode.getGuard());
    forNode.getInitialize().accept(this);
    forNode.getUpdate().accept(this);
    forNode.getBody().accept(this);
    return null;
  }

  @Override
  public Void visit(BlockNode blockNode) {
    this.symbolTable.push();

    for (StmtNode stmt : blockNode) {
      stmt.accept(this);
    }

    this.symbolTable.pop();

    return null;
  }

  @Override
  public Void visit(AssertNode assertNode) {
    visitGuard(assertNode, assertNode.getExpression());
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
