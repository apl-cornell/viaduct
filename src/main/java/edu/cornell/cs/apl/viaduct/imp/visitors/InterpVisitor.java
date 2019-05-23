package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.imp.ImpAnnotation;
import edu.cornell.cs.apl.viaduct.imp.ImpAnnotations;
import edu.cornell.cs.apl.viaduct.imp.ast.AndNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AnnotationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssignNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BooleanLiteralNode;
import edu.cornell.cs.apl.viaduct.imp.ast.DowngradeNode;
import edu.cornell.cs.apl.viaduct.imp.ast.EqualNode;
import edu.cornell.cs.apl.viaduct.imp.ast.IfNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpValue;
import edu.cornell.cs.apl.viaduct.imp.ast.IntegerLiteralNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LeqNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LessThanNode;
import edu.cornell.cs.apl.viaduct.imp.ast.NotNode;
import edu.cornell.cs.apl.viaduct.imp.ast.OrNode;
import edu.cornell.cs.apl.viaduct.imp.ast.PlusNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReadNode;
import edu.cornell.cs.apl.viaduct.imp.ast.RecvNode;
import edu.cornell.cs.apl.viaduct.imp.ast.SendNode;
import edu.cornell.cs.apl.viaduct.imp.ast.SkipNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;
import edu.cornell.cs.apl.viaduct.imp.ast.VarDeclNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/** interpret an IMP program. */
public class InterpVisitor implements ExprVisitor<ImpValue>, StmtVisitor<Void> {

  private static final String DEFAULT_HOST = "__DEFAULT__";
  private static final String INPUT_CHAN = "input";
  private static final String OUTPUT_CHAN = "output";
  String host;
  Map<Variable, ImpValue> store;
  Map<String, Map<String, Queue<ImpValue>>> msgQueues;
  boolean multiprocess;
  Lock storeLock;
  Map<String, Map<String, Condition>> nonemptyQueueConds;

  public InterpVisitor() {
    host = DEFAULT_HOST;
    this.multiprocess = false;
  }

  private InterpVisitor(
      String h,
      Lock sl,
      Map<String, Map<String, Queue<ImpValue>>> q,
      Map<String, Map<String, Condition>> nqc) {

    this.store = new HashMap<>();
    this.host = h;
    this.msgQueues = q;
    this.multiprocess = true;
    this.storeLock = sl;
    this.nonemptyQueueConds = nqc;
  }

  public InterpVisitor spawnChild(String host) {
    return new InterpVisitor(host, this.storeLock, this.msgQueues, this.nonemptyQueueConds);
  }

  /** compute process configuration using annotations in the program. */
  private Map<String, StmtNode> getProcessConfig(BlockNode program) {
    Map<String, StmtNode> processConfig = new HashMap<>();

    String curHost = DEFAULT_HOST;
    List<StmtNode> curBlock = new ArrayList<>();
    for (StmtNode stmt : program) {
      boolean newProcess = false;
      if (stmt instanceof AnnotationNode) {
        AnnotationNode annotNode = (AnnotationNode) stmt;
        ImpAnnotation annot = annotNode.getAnnotation();

        // demarcation of new process
        if (annot instanceof ImpAnnotations.ProcessAnnotation) {
          ImpAnnotations.ProcessAnnotation procAnnot = (ImpAnnotations.ProcessAnnotation) annot;

          // only add process mapping if there is something to execute!
          if (curBlock.size() > 0) {
            processConfig.put(curHost, new BlockNode(curBlock));
          }

          curHost = procAnnot.getHost();
          curBlock = new ArrayList<>();
          newProcess = true;
        }
      }

      // no new process demarcation, add stmt to current block
      if (!newProcess) {
        curBlock.add(stmt);
      }
    }

    if (curBlock.size() > 0) {
      processConfig.put(curHost, new BlockNode(curBlock));
    }

    return processConfig;
  }

  /** interpret program. */
  public Map<String, Map<Variable, ImpValue>> interpret(StmtNode stmt) throws InterruptedException {

    this.storeLock = new ReentrantLock();
    this.msgQueues = new HashMap<>();
    this.nonemptyQueueConds = new HashMap<>();
    this.multiprocess = false;

    Map<String, StmtNode> processConfig = null;
    if (stmt instanceof BlockNode) {
      BlockNode programBlock = (BlockNode) stmt;
      processConfig = getProcessConfig(programBlock);
      int configSize = processConfig.size();
      this.multiprocess = configSize > 1;

      if (configSize == 1) {
        this.host = (String) processConfig.keySet().toArray()[0];
      }

    } else {
      processConfig = new HashMap<>();
    }

    Set<String> hosts = processConfig.keySet();
    Set<InterpThread> children = new HashSet<>();

    for (Map.Entry<String, StmtNode> kv : processConfig.entrySet()) {
      // build message queue and map of conditional vars
      Map<String, Queue<ImpValue>> msgQueue = new HashMap<>();
      Map<String, Condition> nonemptyQueueCond = new HashMap<>();

      // have an input and output queue
      msgQueue.put(INPUT_CHAN, new LinkedList<>());
      msgQueue.put(OUTPUT_CHAN, new LinkedList<>());
      nonemptyQueueCond.put(INPUT_CHAN, this.storeLock.newCondition());
      nonemptyQueueCond.put(OUTPUT_CHAN, this.storeLock.newCondition());

      // have a queue for every host
      String host = kv.getKey();
      for (String otherHost : hosts) {
        if (!otherHost.equals(host)) {
          msgQueue.put(otherHost, new LinkedList<>());
          nonemptyQueueCond.put(otherHost, this.storeLock.newCondition());
        }
      }

      msgQueues.put(host, msgQueue);
      nonemptyQueueConds.put(host, nonemptyQueueCond);

      // create new thread per host
      if (this.multiprocess) {
        StmtNode hostProgram = kv.getValue();
        InterpThread childThread = new InterpThread(this, host, hostProgram);
        children.add(childThread);
      }
    }

    Map<String, Map<Variable, ImpValue>> storeMap = new HashMap<>();

    // create a thread per host
    if (this.multiprocess) {
      for (InterpThread childThread : children) {
        childThread.start();
      }

      for (InterpThread childThread : children) {
        childThread.join();
        InterpVisitor childInterpreter = childThread.getInterpreter();
        storeMap.put(childThread.getHost(), childInterpreter.store);
      }

      // only need a single process to interpret the program
    } else {
      this.store = new HashMap<>();
      stmt.accept(this);
      storeMap.put(this.host, this.store);
    }

    return storeMap;
  }

  private void sendFrom(String sender, String recipient, ImpValue val) {
    this.storeLock.lock();

    try {
      Queue<ImpValue> msgQueue = this.msgQueues.get(recipient).get(sender);
      Condition nonemptyCond = this.nonemptyQueueConds.get(recipient).get(sender);

      if (msgQueue != null) {
        msgQueue.add(val);
        nonemptyCond.signalAll();
      }

    } finally {
      this.storeLock.unlock();
    }
  }

  /** read a variable from the store. */
  @Override
  public ImpValue visit(ReadNode readNode) {
    ImpValue val = this.store.get(readNode.getVariable());
    return val;
  }

  @Override
  public ImpValue visit(IntegerLiteralNode integerLiteralNode) {
    return integerLiteralNode;
  }

  /** interpret plus node. */
  @Override
  public ImpValue visit(PlusNode plusNode) {
    IntegerLiteralNode lval = (IntegerLiteralNode) plusNode.getLhs().accept(this);
    IntegerLiteralNode rval = (IntegerLiteralNode) plusNode.getRhs().accept(this);
    return new IntegerLiteralNode(lval.getValue() + rval.getValue());
  }

  @Override
  public ImpValue visit(BooleanLiteralNode booleanLiteralNode) {
    return booleanLiteralNode;
  }

  /** interpret or node. */
  @Override
  public ImpValue visit(OrNode orNode) {
    BooleanLiteralNode lval = (BooleanLiteralNode) orNode.getLhs().accept(this);
    BooleanLiteralNode rval = (BooleanLiteralNode) orNode.getRhs().accept(this);
    return new BooleanLiteralNode(lval.getValue() || rval.getValue());
  }

  /** interpret and node. */
  @Override
  public ImpValue visit(AndNode andNode) {
    BooleanLiteralNode lval = (BooleanLiteralNode) andNode.getLhs().accept(this);
    BooleanLiteralNode rval = (BooleanLiteralNode) andNode.getRhs().accept(this);
    return new BooleanLiteralNode(lval.getValue() && rval.getValue());
  }

  /** interpret lt node. */
  @Override
  public ImpValue visit(LessThanNode ltNode) {
    IntegerLiteralNode lval = (IntegerLiteralNode) ltNode.getLhs().accept(this);
    IntegerLiteralNode rval = (IntegerLiteralNode) ltNode.getRhs().accept(this);
    return new BooleanLiteralNode(lval.getValue() < rval.getValue());
  }

  /** interpret equals node. */
  @Override
  public ImpValue visit(EqualNode eqNode) {
    IntegerLiteralNode lval = (IntegerLiteralNode) eqNode.getLhs().accept(this);
    IntegerLiteralNode rval = (IntegerLiteralNode) eqNode.getRhs().accept(this);
    return new BooleanLiteralNode(lval.getValue() == rval.getValue());
  }

  /** interpret leq node. */
  @Override
  public ImpValue visit(LeqNode leqNode) {
    IntegerLiteralNode lval = (IntegerLiteralNode) leqNode.getLhs().accept(this);
    IntegerLiteralNode rval = (IntegerLiteralNode) leqNode.getRhs().accept(this);
    return new BooleanLiteralNode(lval.getValue() <= rval.getValue());
  }

  @Override
  public ImpValue visit(NotNode notNode) {
    BooleanLiteralNode val = (BooleanLiteralNode) notNode.getExpression().accept(this);
    return new BooleanLiteralNode(!val.getValue());
  }

  @Override
  public ImpValue visit(DowngradeNode downgradeNode) {
    return downgradeNode.getExpression().accept(this);
  }

  @Override
  public Void visit(SkipNode skipNode) {
    return null;
  }

  @Override
  public Void visit(VarDeclNode varDeclNode) {
    this.store.put(varDeclNode.getVariable(), null);
    return null;
  }

  /** interpret assignment node. */
  @Override
  public Void visit(AssignNode assignNode) {
    ImpValue rhsVal = assignNode.getRhs().accept(this);
    this.store.put(assignNode.getVariable(), rhsVal);
    return null;
  }

  /** interpret block node. */
  @Override
  public Void visit(BlockNode blockNode) {
    for (StmtNode stmt : blockNode) {
      stmt.accept(this);
    }
    return null;
  }

  /** interpret conditional node. */
  @Override
  public Void visit(IfNode ifNode) {
    BooleanLiteralNode guardVal = (BooleanLiteralNode) ifNode.getGuard().accept(this);
    if (guardVal.getValue()) {
      ifNode.getThenBranch().accept(this);
    } else {
      ifNode.getElseBranch().accept(this);
    }
    return null;
  }

  /** send value to another host. */
  @Override
  public Void visit(SendNode sendNode) {
    ImpValue sentVal = sendNode.getSentExpr().accept(this);
    sendFrom(this.host, sendNode.getRecipient(), sentVal);
    return null;
  }

  /** send value to another host. */
  @Override
  public Void visit(RecvNode recvNode) {
    this.storeLock.lock();

    try {
      String sender = recvNode.getSender();
      Queue<ImpValue> msgQueue = this.msgQueues.get(this.host).get(sender);
      Condition nonemptyCond = this.nonemptyQueueConds.get(this.host).get(sender);
      while (msgQueue.isEmpty()) {
        nonemptyCond.await();
      }

      ImpValue val = msgQueue.remove();
      Variable var = recvNode.getVar();
      this.store.put(var, val);

    } catch (Exception e) {
      throw new RuntimeException(e);

    } finally {
      this.storeLock.unlock();
    }

    return null;
  }

  /** execute annotation. */
  @Override
  public Void visit(AnnotationNode annotNode) {
    ImpAnnotation annot = annotNode.getAnnotation();
    if (annot != null) {
      // interpret statement in annotation
      if (annot instanceof ImpAnnotations.InterpAnnotation) {
        ImpAnnotations.InterpAnnotation interpAnnot = (ImpAnnotations.InterpAnnotation) annot;
        interpAnnot.getProgram().accept(this);

        // put value in host's input channel
      } else if (annot instanceof ImpAnnotations.InputAnnotation) {
        ImpAnnotations.InputAnnotation inputAnnot = (ImpAnnotations.InputAnnotation) annot;
        sendFrom(INPUT_CHAN, this.host, inputAnnot.getValue());
      }
    }

    return null;
  }

  private void printMsgQueues() {
    for (Map.Entry<String, Map<String, Queue<ImpValue>>> kv : this.msgQueues.entrySet()) {

      String recipient = kv.getKey();
      Map<String, Queue<ImpValue>> msgQueue = kv.getValue();
      for (Map.Entry<String, Queue<ImpValue>> rkv : msgQueue.entrySet()) {
        Queue<ImpValue> senderQueue = rkv.getValue();
        System.out.println("recipient: " + recipient + ", sender: " + rkv.getKey());
        if (senderQueue.size() > 0) {
          for (ImpValue val : senderQueue) {
            System.out.println(val);
          }
        } else {
          System.out.println("empty!");
        }
      }
    }
  }

  static class InterpThread extends Thread {
    String host;
    InterpVisitor interpreter;
    StmtNode program;

    public InterpThread(InterpVisitor i, String h, StmtNode p) {
      this.host = h;
      this.interpreter = i.spawnChild(this.host);
      this.program = p;
    }

    @Override
    public void run() {
      this.program.accept(this.interpreter);
    }

    public String getHost() {
      return this.host;
    }

    public InterpVisitor getInterpreter() {
      return this.interpreter;
    }
  }
}
