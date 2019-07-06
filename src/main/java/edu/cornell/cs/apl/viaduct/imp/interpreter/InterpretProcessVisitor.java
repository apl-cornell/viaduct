package edu.cornell.cs.apl.viaduct.imp.interpreter;

import edu.cornell.cs.apl.viaduct.imp.ElaborationException;
import edu.cornell.cs.apl.viaduct.imp.ast.ArrayDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ArrayIndex;
import edu.cornell.cs.apl.viaduct.imp.ast.AssertNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssignNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BinaryExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BooleanValue;
import edu.cornell.cs.apl.viaduct.imp.ast.BreakNode;
import edu.cornell.cs.apl.viaduct.imp.ast.DowngradeNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ForNode;
import edu.cornell.cs.apl.viaduct.imp.ast.IfNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpValue;
import edu.cornell.cs.apl.viaduct.imp.ast.IntegerValue;
import edu.cornell.cs.apl.viaduct.imp.ast.LetBindingNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LiteralNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LoopNode;
import edu.cornell.cs.apl.viaduct.imp.ast.NotNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessName;
import edu.cornell.cs.apl.viaduct.imp.ast.ReadNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReceiveNode;
import edu.cornell.cs.apl.viaduct.imp.ast.SendNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.imp.ast.VariableDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.WhileNode;
import edu.cornell.cs.apl.viaduct.imp.visitors.ExprVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.ReferenceVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;
import java.util.Objects;

class InterpretProcessVisitor implements ExprVisitor<ImpValue>, StmtVisitor<Void> {

  /** The process to execute the statements as. */
  private final ProcessName processName;

  /** The (multi-way) channel that connects {@code processName} to all other processes. */
  private final Channel<ImpValue> channel;

  /** Maps variables to their values. */
  private final Store store = new Store();

  /**
   * Create a new interpreter that acts as {@code processName} and uses {@code channel} to
   * communicate.
   *
   * @param process the process to run the statements as
   * @param channel connects {@code processName} to all other hosts involved in the computation
   */
  InterpretProcessVisitor(ProcessName process, Channel<ImpValue> channel) {
    this.processName = Objects.requireNonNull(process);
    this.channel = Objects.requireNonNull(channel);
  }

  /**
   * Create a new interpreter that can only evaluate statements that do not send or receive values.
   */
  InterpretProcessVisitor() {
    this.processName = null;
    this.channel = null;
  }

  /**
   * Given a value that is meant to be an index into an array, check that it can be cast to an
   * integer and return this integer. Throw an appropriate exception otherwise.
   */
  private static int getIntValueOfIndex(ImpValue impValue, Variable array)
      throws NonIntegerIndexException {
    if (impValue instanceof IntegerValue) {
      return ((IntegerValue) impValue).getValue();
    } else {
      throw new NonIntegerIndexException(array, impValue);
    }
  }

  /** Return the value of the given expression in the current context. */
  ImpValue run(ExpressionNode expression) {
    return expression.accept(this);
  }

  /**
   * Interpreter the given statement and return the resulting store.
   *
   * <p>If this function is called multiple times, it will behave as if the statements were
   * sequentially composed.
   */
  Store run(StmtNode statement) {
    statement.accept(this);
    return store;
  }

  @Override
  public ImpValue visit(LiteralNode literalNode) {
    return literalNode.getValue();
  }

  @Override
  public ImpValue visit(ReadNode readNode) {
    return readNode.getReference().accept(new ReadReferenceVisitor());
  }

  @Override
  public ImpValue visit(NotNode notNode) {
    BooleanValue val = (BooleanValue) notNode.getExpression().accept(this);
    return new BooleanValue(!val.getValue());
  }

  @Override
  public ImpValue visit(BinaryExpressionNode binaryExpressionNode) {
    ImpValue left = binaryExpressionNode.getLhs().accept(this);
    ImpValue right = binaryExpressionNode.getRhs().accept(this);
    return binaryExpressionNode.getOperator().evaluate(left, right);
  }

  @Override
  public ImpValue visit(DowngradeNode downgradeNode) {
    return downgradeNode.getExpression().accept(this);
  }

  @Override
  public Void visit(VariableDeclarationNode variableDeclarationNode) {
    store.declare(variableDeclarationNode.getVariable());
    return null;
  }

  @Override
  public Void visit(ArrayDeclarationNode arrayDeclNode) {
    Variable array = arrayDeclNode.getVariable();
    ImpValue lengthValue = arrayDeclNode.getLength().accept(this);
    int length = getIntValueOfIndex(lengthValue, array);
    store.declareArray(array, length);
    return null;
  }

  @Override
  public Void visit(LetBindingNode letBindingNode) {
    ImpValue value = letBindingNode.getRhs().accept(this);
    store.declareTemp(letBindingNode.getVariable(), value);
    return null;
  }

  @Override
  public Void visit(AssignNode assignNode) {
    ImpValue value = assignNode.getRhs().accept(this);
    assignNode.getLhs().accept(new WriteReferenceVisitor(value));
    return null;
  }

  @Override
  public Void visit(SendNode sendNode) {
    ImpValue value = sendNode.getSentExpression().accept(this);
    try {
      this.channel.send(this.processName, sendNode.getRecipient(), value);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    return null;
  }

  @Override
  public Void visit(ReceiveNode receiveNode) {
    ImpValue value;

    try {
      value = this.channel.receive(receiveNode.getSender(), this.processName);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    store.update(receiveNode.getVariable(), value);

    return null;
  }

  @Override
  public Void visit(IfNode ifNode) {
    BooleanValue guardVal = (BooleanValue) ifNode.getGuard().accept(this);
    if (guardVal.getValue()) {
      ifNode.getThenBranch().accept(this);
    } else {
      ifNode.getElseBranch().accept(this);
    }
    return null;
  }

  @Override
  public Void visit(WhileNode whileNode) {
    BooleanValue guardVal = (BooleanValue) whileNode.getGuard().accept(this);
    if (guardVal.getValue()) {
      whileNode.getBody().accept(this);
      visit(whileNode);
    }

    return null;
  }

  @Override
  public Void visit(ForNode forNode) {
    throw new ElaborationException();
  }

  @Override
  public Void visit(LoopNode loopNode) {
    loopNode.getBody().accept(this);
    return null;
  }

  @Override
  public Void visit(BreakNode breakNode) {
    // TODO: do the right thing
    return null;
  }

  @Override
  public Void visit(BlockNode blockNode) {
    store.pushTempContext();
    for (StmtNode stmt : blockNode) {
      stmt.accept(this);
    }
    store.popTempContext();
    return null;
  }

  @Override
  public Void visit(AssertNode assertNode) {
    ExpressionNode assertExpr = assertNode.getExpression();
    ImpValue assertion = assertExpr.accept(this);

    if (!((BooleanValue) assertion).getValue()) {
      throw new AssertionFailureException(assertExpr);
    }
    return null;
  }

  /** Read the value stored at a reference. */
  private class ReadReferenceVisitor implements ReferenceVisitor<ImpValue> {
    @Override
    public ImpValue visit(Variable variable) {
      return store.lookup(variable);
    }

    @Override
    public ImpValue visit(ArrayIndex arrayIndex) {
      ImpValue indexValue = arrayIndex.getIndex().accept(InterpretProcessVisitor.this);
      int index = getIntValueOfIndex(indexValue, arrayIndex.getArray());
      return store.lookupArray(arrayIndex.getArray(), index);
    }
  }

  /** Update the value stored at a reference. */
  private class WriteReferenceVisitor implements ReferenceVisitor<Void> {
    private final ImpValue newValue;

    WriteReferenceVisitor(ImpValue newValue) {
      this.newValue = Objects.requireNonNull(newValue);
    }

    @Override
    public Void visit(Variable variable) {
      store.update(variable, newValue);
      return null;
    }

    @Override
    public Void visit(ArrayIndex arrayIndex) {
      ImpValue indexValue = arrayIndex.getIndex().accept(InterpretProcessVisitor.this);
      int index = getIntValueOfIndex(indexValue, arrayIndex.getArray());
      store.updateArray(arrayIndex.getArray(), index, newValue);
      return null;
    }
  }
}
