package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.imp.ElaborationException;
import edu.cornell.cs.apl.viaduct.imp.ast.ArrayDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ArrayIndex;
import edu.cornell.cs.apl.viaduct.imp.ast.AssertNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssignNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BinaryExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.DowngradeNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ForNode;
import edu.cornell.cs.apl.viaduct.imp.ast.IfNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LetBindingNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LiteralNode;
import edu.cornell.cs.apl.viaduct.imp.ast.NotNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReadNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReceiveNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Reference;
import edu.cornell.cs.apl.viaduct.imp.ast.SendNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.imp.ast.VariableDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.WhileNode;
import edu.cornell.cs.apl.viaduct.util.FreshNameGenerator;
import edu.cornell.cs.apl.viaduct.util.SymbolTable;
import java.util.ArrayList;
import java.util.List;

/** translation to A-normal form. */
public class AnfVisitor extends FormatBlockVisitor {
  private static final String TMP_NAME = "tmp";
  private final FreshNameGenerator nameGenerator;
  private final SymbolTable<Variable, Boolean> declaredVars;
  private final ANFChecker anfChecker = new ANFChecker();
  private final AtomicChecker atomicChecker = new AtomicChecker();
  private List<LetBindingNode> currentBindings;

  /** constructor. */
  public AnfVisitor() {
    this.nameGenerator = new FreshNameGenerator();
    this.declaredVars = new SymbolTable<>();
    this.currentBindings = new ArrayList<>();
  }

  private List<LetBindingNode> flushBindingMap() {
    List<LetBindingNode> oldBindingList = new ArrayList<>(this.currentBindings);
    this.currentBindings.clear();
    return oldBindingList;
  }

  private StmtNode prependBindings(StmtNode stmt) {
    List<StmtNode> stmtList = new ArrayList<>();
    stmtList.addAll(flushBindingMap());
    stmtList.add(stmt);
    return BlockNode.create(stmtList);
  }

  private ReadNode addBinding(ExpressionNode expr) {
    Variable tmpVar = Variable.create(this.nameGenerator.getFreshName(TMP_NAME));
    LetBindingNode letBinding = LetBindingNode.create(tmpVar, expr);
    this.currentBindings.add(letBinding);
    Reference newVar = Variable.create(tmpVar);
    return ReadNode.create(newVar);
  }

  @Override
  public Reference visit(Variable var) {
    return var;
  }

  @Override
  public Reference visit(ArrayIndex arrayInd) {
    ExpressionNode newIndex = arrayInd.getIndex().accept(this);
    return ArrayIndex.create(arrayInd.getArray(), newIndex);
  }

  @Override
  public ExpressionNode visit(LiteralNode literalNode) {
    return literalNode;
  }

  @Override
  public ExpressionNode visit(ReadNode readNode) {
    Reference oldRef = readNode.getReference();

    if (this.atomicChecker.run(oldRef)) {
      return readNode;

    } else {
      Reference newRef = oldRef.accept(this);
      return addBinding(ReadNode.create(newRef));
    }
  }

  @Override
  public ExpressionNode visit(NotNode notNode) {
    ExpressionNode newExpr = notNode.getExpression().accept(this);
    return addBinding(NotNode.create(newExpr));
  }

  @Override
  public ExpressionNode visit(BinaryExpressionNode binaryExpressionNode) {
    ExpressionNode newLhs = binaryExpressionNode.getLhs().accept(this);
    ExpressionNode newRhs = binaryExpressionNode.getRhs().accept(this);
    ExpressionNode newBinop =
        BinaryExpressionNode.create(newLhs, binaryExpressionNode.getOperator(), newRhs);
    return addBinding(newBinop);
  }

  @Override
  public ExpressionNode visit(DowngradeNode downgradeNode) {
    ExpressionNode newExpr = downgradeNode.getExpression().accept(this);
    ExpressionNode newDowngrade =
        DowngradeNode.create(
            newExpr,
            downgradeNode.getFromLabel(),
            downgradeNode.getLabel(),
            downgradeNode.getDowngradeType());
    return addBinding(newDowngrade);
  }

  @Override
  public StmtNode visit(VariableDeclarationNode varDeclNode) {
    this.declaredVars.put(varDeclNode.getVariable(), true);
    return VariableDeclarationNode.create(
        varDeclNode.getVariable(), varDeclNode.getType(), varDeclNode.getLabel());
  }

  @Override
  public StmtNode visit(ArrayDeclarationNode arrayDeclNode) {
    this.declaredVars.put(arrayDeclNode.getVariable(), true);
    ExpressionNode newLength = arrayDeclNode.getLength().accept(this);
    StmtNode newStmt =
        ArrayDeclarationNode.create(
            arrayDeclNode.getVariable(),
            newLength,
            arrayDeclNode.getType(),
            arrayDeclNode.getLabel());
    return prependBindings(newStmt);
  }

  @Override
  public StmtNode visit(LetBindingNode letBindingNode) {
    ExpressionNode oldRhs = letBindingNode.getRhs();
    ExpressionNode newRhs = this.anfChecker.run(oldRhs) ? oldRhs : oldRhs.accept(this);
    return prependBindings(LetBindingNode.create(letBindingNode.getVariable(), newRhs));
  }

  @Override
  public StmtNode visit(AssignNode assignNode) {
    Reference newLhs = assignNode.getLhs().accept(this);
    ExpressionNode newRhs = assignNode.getRhs().accept(this);
    return prependBindings(AssignNode.create(newLhs, newRhs));
  }

  @Override
  public StmtNode visit(SendNode sendNode) {
    ExpressionNode newExpr = sendNode.getSentExpression().accept(this);
    return prependBindings(SendNode.create(sendNode.getRecipient(), newExpr));
  }

  @Override
  public StmtNode visit(ReceiveNode recvNode) {
    Variable recvVar = recvNode.getVariable();

    // are we assigning to a temporary or a declared var?
    // if we are assigning to a declared var, create a new temp
    // and bind that first before assiging to the declared var
    if (this.atomicChecker.run(recvVar)) {
      return recvNode;

    } else {
      Variable tmpVar = Variable.create(this.nameGenerator.getFreshName(TMP_NAME));
      List<StmtNode> stmtList = new ArrayList<>();
      stmtList.add(ReceiveNode.create(tmpVar, recvNode.getRecvType(), recvNode.getSender()));
      stmtList.add(AssignNode.create(recvVar, ReadNode.create(tmpVar)));
      return BlockNode.create(stmtList);
    }
  }

  @Override
  public StmtNode visit(IfNode ifNode) {
    ExpressionNode newGuard = ifNode.getGuard().accept(this);
    List<LetBindingNode> guardBindings = flushBindingMap();

    StmtNode newThen = ifNode.getThenBranch().accept(this);
    StmtNode newElse = ifNode.getElseBranch().accept(this);

    List<StmtNode> stmtList = new ArrayList<>();
    stmtList.addAll(guardBindings);
    stmtList.add(IfNode.create(newGuard, newThen, newElse));
    return BlockNode.create(stmtList);
  }

  @Override
  public StmtNode visit(WhileNode whileNode) {
    throw new ElaborationException();
  }

  @Override
  public StmtNode visit(ForNode forNode) {
    throw new ElaborationException();
  }

  @Override
  public StmtNode visit(BlockNode blockNode) {
    // flatten nested blocks
    this.declaredVars.push();
    StmtNode newBlock = super.visit(blockNode);
    this.declaredVars.pop();

    return newBlock;
  }

  @Override
  public StmtNode visit(AssertNode assertNode) {
    ExpressionNode newExpr = assertNode.getExpression().accept(this);
    return prependBindings(AssertNode.create(newExpr));
  }

  /** check if an expression is in ANF. */
  class ANFChecker implements ExprVisitor<Boolean>, ReferenceVisitor<Boolean> {
    public Boolean run(ExpressionNode expr) {
      return expr.accept(this);
    }

    public Boolean run(Reference ref) {
      return ref.accept(this);
    }

    @Override
    public Boolean visit(Variable var) {
      return true;
    }

    @Override
    public Boolean visit(ArrayIndex arrayIndex) {
      return AnfVisitor.this.atomicChecker.run(arrayIndex.getIndex());
    }

    @Override
    public Boolean visit(LiteralNode literalNode) {
      return true;
    }

    @Override
    public Boolean visit(ReadNode readNode) {
      return readNode.getReference().accept(this);
    }

    @Override
    public Boolean visit(NotNode notNode) {
      return AnfVisitor.this.atomicChecker.run(notNode.getExpression());
    }

    @Override
    public Boolean visit(BinaryExpressionNode binExprNode) {
      Boolean lhsAnf = AnfVisitor.this.atomicChecker.run(binExprNode.getLhs());
      Boolean rhsAnf = AnfVisitor.this.atomicChecker.run(binExprNode.getRhs());
      return lhsAnf && rhsAnf;
    }

    @Override
    public Boolean visit(DowngradeNode downgradeNode) {
      return AnfVisitor.this.atomicChecker.run(downgradeNode.getExpression());
    }
  }

  class AtomicChecker implements ExprVisitor<Boolean>, ReferenceVisitor<Boolean> {
    public Boolean run(ExpressionNode expr) {
      return expr.accept(this);
    }

    public Boolean run(Reference ref) {
      return ref.accept(this);
    }

    @Override
    public Boolean visit(Variable var) {
      return !AnfVisitor.this.declaredVars.contains(var);
    }

    @Override
    public Boolean visit(ArrayIndex arrayIndex) {
      return false;
    }

    @Override
    public Boolean visit(LiteralNode literalNode) {
      return true;
    }

    @Override
    public Boolean visit(ReadNode readNode) {
      return readNode.getReference().accept(this);
    }

    @Override
    public Boolean visit(NotNode notNode) {
      return false;
    }

    @Override
    public Boolean visit(BinaryExpressionNode binExprNode) {
      return false;
    }

    @Override
    public Boolean visit(DowngradeNode downgradeNode) {
      return false;
    }
  }
}
