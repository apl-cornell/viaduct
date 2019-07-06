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
public class ANFVisitor extends FormatBlockVisitor {
  private static final String TMP_NAME = "tmp";
  private final FreshNameGenerator nameGenerator;
  private final SymbolTable<Variable,Boolean> declaredVars;
  private List<LetBindingNode> currentBindings;

  /** constructor. */
  public ANFVisitor() {
    this.nameGenerator = new FreshNameGenerator();
    this.declaredVars = new SymbolTable<>();
    this.currentBindings = new ArrayList<>();
  }

  private List<LetBindingNode> flushBindingMap() {
    List<LetBindingNode> oldBindingList = new ArrayList<>(this.currentBindings);
    this.currentBindings.clear();
    return oldBindingList;
  }

  private BlockNode appendBindings(StmtNode stmt) {
    List<StmtNode> stmtList = new ArrayList<>();
    stmtList.addAll(flushBindingMap());
    stmtList.add(stmt);
    return new BlockNode(stmtList);
  }

  private ReadNode addBinding(ExpressionNode expr) {
    Variable tmpVar = new Variable(this.nameGenerator.getFreshName(TMP_NAME));
    LetBindingNode letBinding = new LetBindingNode(tmpVar, expr);
    this.currentBindings.add(letBinding);
    return new ReadNode(new Variable(tmpVar));
  }

  @Override
  public Reference visit(Variable var) {
    return var;
  }

  @Override
  public Reference visit(ArrayIndex arrayInd) {
    ExpressionNode newIndex = arrayInd.getIndex().accept(this);
    return new ArrayIndex(arrayInd.getArray(), newIndex);
  }

  @Override
  public ExpressionNode visit(LiteralNode literalNode) {
    return literalNode;
  }

  @Override
  public ExpressionNode visit(ReadNode readNode) {
    Reference ref = readNode.getReference();

    if (ref instanceof Variable) {
      Variable refVar = (Variable)ref;

      // treat non-temp variable read as an object query
      if (this.declaredVars.contains(refVar)) {
        return addBinding(readNode);

      } else {
        return readNode;
      }

    } else { // array index
      ArrayIndex arrayInd = (ArrayIndex)ref;
      ExpressionNode newInd = arrayInd.getIndex().accept(this);
      ReadNode newRead = new ReadNode(new ArrayIndex(arrayInd.getArray(), newInd));
      return addBinding(newRead);
    }
  }

  @Override
  public ExpressionNode visit(NotNode notNode) {
    ExpressionNode newExpr = notNode.getExpression().accept(this);
    return addBinding(new NotNode(newExpr));
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
    ExpressionNode newDowngrade = new DowngradeNode(newExpr, downgradeNode.getLabel());
    return addBinding(newDowngrade);
  }

  @Override
  public StmtNode visit(VariableDeclarationNode varDeclNode) {
    this.declaredVars.add(varDeclNode.getVariable(), true);
    return new VariableDeclarationNode(
        varDeclNode.getVariable(),
        varDeclNode.getType(),
        varDeclNode.getLabel());
  }

  @Override
  public StmtNode visit(ArrayDeclarationNode arrayDeclNode) {
    this.declaredVars.add(arrayDeclNode.getVariable(), true);
    ExpressionNode newLength = arrayDeclNode.getLength().accept(this);
    StmtNode newStmt = new ArrayDeclarationNode(
        arrayDeclNode.getVariable(),
        newLength,
        arrayDeclNode.getType(),
        arrayDeclNode.getLabel());
    return appendBindings(newStmt);
  }

  @Override
  public StmtNode visit(LetBindingNode letBindingNode) {
    ExpressionNode newRhs = letBindingNode.getRhs().accept(this);
    return appendBindings(new LetBindingNode(letBindingNode.getVariable(), newRhs));
  }

  @Override
  public StmtNode visit(AssignNode assignNode) {
    Reference newRef = assignNode.getLhs().accept(this);
    ExpressionNode newRhs = assignNode.getRhs().accept(this);
    return appendBindings(new AssignNode(newRef, newRhs));
  }

  @Override
  public StmtNode visit(SendNode sendNode) {
    ExpressionNode newExpr = sendNode.getSentExpression().accept(this);
    return appendBindings(new SendNode(sendNode.getRecipient(), newExpr));
  }

  @Override
  public StmtNode visit(ReceiveNode receiveNode) {
    return receiveNode;
  }

  @Override
  public StmtNode visit(IfNode ifNode) {
    ExpressionNode newGuard = ifNode.getGuard().accept(this);
    List<LetBindingNode> guardBindings = flushBindingMap();

    StmtNode newThen = ifNode.getThenBranch().accept(this);
    StmtNode newElse = ifNode.getElseBranch().accept(this);

    List<StmtNode> stmtList = new ArrayList<>();
    stmtList.addAll(guardBindings);
    stmtList.add(new IfNode(newGuard, newThen, newElse));
    return new BlockNode(stmtList);
  }

  @Override
  public StmtNode visit(WhileNode whileNode) {
    // TODO: figure out how loop guards get converted to A-normal form...
    // they're not as straightforward as translating guards for conditionals
    // since the guard is evaluated multiple times
    StmtNode newBody = whileNode.getBody().accept(this);
    return new WhileNode(whileNode.getGuard(), newBody);
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
    return appendBindings(new AssertNode(newExpr));
  }
}
