package edu.cornell.cs.apl.viaduct.imp.interpreter;

import edu.cornell.cs.apl.viaduct.imp.ElaborationException;
import edu.cornell.cs.apl.viaduct.imp.ast.ArrayAccessNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ArrayDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ArrayIndexNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ArrayIndexValue;
import edu.cornell.cs.apl.viaduct.imp.ast.AssertNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssignNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BinaryExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BooleanValue;
import edu.cornell.cs.apl.viaduct.imp.ast.DeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.DowngradeNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ForNode;
import edu.cornell.cs.apl.viaduct.imp.ast.IfNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpLValue;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpValue;
import edu.cornell.cs.apl.viaduct.imp.ast.IntegerValue;
import edu.cornell.cs.apl.viaduct.imp.ast.LReadNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LiteralNode;
import edu.cornell.cs.apl.viaduct.imp.ast.NotNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessName;
import edu.cornell.cs.apl.viaduct.imp.ast.ReadNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReceiveNode;
import edu.cornell.cs.apl.viaduct.imp.ast.SendNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.imp.ast.WhileNode;
import edu.cornell.cs.apl.viaduct.imp.visitors.ExprVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.LExprVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;
import java.util.Objects;

class InterpretProcessVisitor
    implements ExprVisitor<ImpValue>, StmtVisitor<Void>, LExprVisitor<ImpLValue> {

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
    try {
      return store.lookup(readNode.getVariable());
    } catch (UndeclaredVariableException | UnassignedVariableException e) {
      throw new Error(e);
    }
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
  public ImpValue visit(ArrayAccessNode arrAccessNode) {
    Variable var = arrAccessNode.getVariable();
    ImpValue indexVal = arrAccessNode.getIndex().accept(this);

    try {
      if (indexVal instanceof IntegerValue) {
        int index = ((IntegerValue) indexVal).getValue();
        return this.store.lookupArray(var, index);

      } else {
        throw new NonIntegerIndexException(var, indexVal);
      }
    } catch (Exception e) {
      throw new Error(e);
    }
  }

  @Override
  public ImpLValue visit(LReadNode lreadNode) {
    return lreadNode.getVariable();
  }

  @Override
  public ImpLValue visit(ArrayIndexNode arrAccessNode) {
    Variable var = arrAccessNode.getVariable();
    ImpValue indexVal = arrAccessNode.getIndex().accept(this);

    try {
      if (indexVal instanceof IntegerValue) {
        int index = ((IntegerValue) indexVal).getValue();
        return new ArrayIndexValue(var, index);

      } else {
        throw new NonIntegerIndexException(var, indexVal);
      }
    } catch (Exception e) {
      throw new Error(e);
    }
  }

  @Override
  public Void visit(DeclarationNode declarationNode) {
    try {
      store.declare(declarationNode.getVariable());
    } catch (RedeclaredVariableException e) {
      throw new Error(e);
    }
    return null;
  }

  @Override
  public Void visit(ArrayDeclarationNode arrayDeclNode) {
    try {
      Variable var = arrayDeclNode.getVariable();
      ImpValue length = arrayDeclNode.getLength().accept(this);

      if (length instanceof IntegerValue) {
        int intLength = ((IntegerValue) length).getValue();
        store.declareArray(var, intLength);

      } else {
        throw new NonIntegerIndexException(var, length);
      }

    } catch (Exception e) {
      throw new Error(e);
    }

    return null;
  }

  @Override
  public Void visit(AssignNode assignNode) {
    ImpLValue lvalue = assignNode.getLhs().accept(this);
    ImpValue value = assignNode.getRhs().accept(this);

    try {
      if (lvalue instanceof Variable) {
        store.update((Variable) lvalue, value);

      } else if (lvalue instanceof ArrayIndexValue) {
        ArrayIndexValue arrIndex = ((ArrayIndexValue) lvalue);
        store.updateArray(arrIndex.getVariable(), arrIndex.getIndex(), value);
      }

    } catch (Exception e) {
      throw new Error(e);
    }

    return null;
  }

  @Override
  public Void visit(SendNode sendNode) {
    ImpValue value = sendNode.getSentExpression().accept(this);
    try {
      this.channel.send(this.processName, sendNode.getRecipient(), value);
    } catch (UnknownProcessException e) {
      throw new Error(e);
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
    } catch (UnknownProcessException e) {
      throw new Error(e);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    try {
      store.update(receiveNode.getVariable(), value);
    } catch (UndeclaredVariableException e) {
      throw new Error(e);
    }

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
    throw new Error(new ElaborationException());
  }

  @Override
  public Void visit(BlockNode blockNode) {
    for (StmtNode stmt : blockNode) {
      stmt.accept(this);
    }
    return null;
  }

  @Override
  public Void visit(AssertNode assertNode) {
    ExpressionNode assertExpr = assertNode.getExpression();
    ImpValue assertion = assertExpr.accept(this);

    if (!((BooleanValue) assertion).getValue()) {
      throw new Error(new AssertionFailureException(assertExpr));
    }
    return null;
  }
}
