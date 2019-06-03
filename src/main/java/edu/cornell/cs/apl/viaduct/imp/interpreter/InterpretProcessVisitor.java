package edu.cornell.cs.apl.viaduct.imp.interpreter;

import edu.cornell.cs.apl.viaduct.imp.ast.ArrayDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssignNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BinaryExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BooleanValue;
import edu.cornell.cs.apl.viaduct.imp.ast.DeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.DowngradeNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Host;
import edu.cornell.cs.apl.viaduct.imp.ast.IfNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpValue;
import edu.cornell.cs.apl.viaduct.imp.ast.LiteralNode;
import edu.cornell.cs.apl.viaduct.imp.ast.NotNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReadNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReceiveNode;
import edu.cornell.cs.apl.viaduct.imp.ast.SendNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;
import edu.cornell.cs.apl.viaduct.imp.visitors.ExprVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;
import java.util.Objects;

class InterpretProcessVisitor implements ExprVisitor<ImpValue>, StmtVisitor<Void> {
  /** The host to execute statements as. */
  private final Host host;

  /** The (multi-way) channel that connects {@code host} to all other hosts. */
  private final Channel<ImpValue> channel;

  /** Maps variables to their values. */
  private final Store store = new Store();

  /**
   * Create a new interpreter that acts as {@code host} and uses {@code channel} to communicate.
   *
   * @param host the host to run statements as.
   * @param channel connects {@code host} to all other hosts involved in the computation.
   */
  InterpretProcessVisitor(Host host, Channel<ImpValue> channel) {
    this.host = Objects.requireNonNull(host);
    this.channel = Objects.requireNonNull(channel);
  }

  /**
   * Create a new interpreter that can only evaluate expression that do not send or receive values.
   */
  InterpretProcessVisitor() {
    this.host = null;
    this.channel = null;
  }

  // TODO: bare interpreter that doesn't require host and channel.

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
  public Void visit(DeclarationNode declarationNode) {
    try {
      store.declare(declarationNode.getVariable());
    } catch (RedeclaredVariableException e) {
      throw new Error(e);
    }
    return null;
  }

  @Override
  public Void visit(ArrayDeclarationNode arrayDeclarationNode) {
    // TODO: implement
    throw new Error("Unimplemented");
  }

  @Override
  public Void visit(AssignNode assignNode) {
    ImpValue value = assignNode.getRhs().accept(this);
    try {
      store.update(assignNode.getVariable(), value);
    } catch (UndeclaredVariableException e) {
      throw new Error(e);
    }
    return null;
  }

  @Override
  public Void visit(SendNode sendNode) {
    ImpValue value = sendNode.getSentExpression().accept(this);
    try {
      this.channel.send(this.host, sendNode.getRecipient(), value);
    } catch (UnknownHostException e) {
      throw new Error(e);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    return null;
  }

  @Override
  public Void visit(ReceiveNode receiveNode) {
    ImpValue value;

    if (channel == null) {
      value = receiveNode.getDebugReceivedValue().accept(this);
    } else {
      try {
        value = this.channel.receive(receiveNode.getSender(), this.host);
      } catch (UnknownHostException e) {
        throw new Error(e);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
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
  public Void visit(BlockNode blockNode) {
    for (StmtNode stmt : blockNode) {
      stmt.accept(this);
    }
    return null;
  }
}
