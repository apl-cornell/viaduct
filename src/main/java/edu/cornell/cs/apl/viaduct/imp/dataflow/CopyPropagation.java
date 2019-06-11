package edu.cornell.cs.apl.viaduct.imp.dataflow;

import edu.cornell.cs.apl.viaduct.dataflow.Dataflow;
import edu.cornell.cs.apl.viaduct.imp.ast.ArrayDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssertNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssignNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.DeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.IfNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReadNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReceiveNode;
import edu.cornell.cs.apl.viaduct.imp.ast.SendNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.imp.visitors.IdentityVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.RenameVisitor;
import edu.cornell.cs.apl.viaduct.security.Lattice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

/** analysis that removes unnecessary temporaries. */
public class CopyPropagation extends Dataflow<CopyPropagation.CopyPropInfo, CFGNode> {
  Map<CFGNode,CopyPropInfo> inputMap;
  Map<CFGNode,CopyPropInfo> outputMap;

  public CopyPropagation() {
    super(DataflowType.FORWARD);
  }

  @Override
  protected CopyPropInfo input(CFGNode node) {
    if (this.inputMap.containsKey(node)) {
      return this.inputMap.get(node);

    } else {
      CopyPropInfo info = new CopyPropInfo();
      this.inputMap.put(node, info);
      return info;
    }
  }

  @Override
  protected CopyPropInfo output(CFGNode node) {
    if (this.outputMap.containsKey(node)) {
      return this.outputMap.get(node);

    } else {
      CopyPropInfo info = new CopyPropInfo();
      this.outputMap.put(node, info);
      return info;
    }
  }

  protected CopyPropInfo transfer(CFGNode node, CopyPropInfo nextInput) {
    return nextInput.kill(node).gen(node);
  }

  protected void updateInput(CFGNode node, CopyPropInfo nextInput) {
    this.inputMap.put(node, nextInput);
  }

  protected void updateOutput(CFGNode node, CopyPropInfo nextInput) {
    this.outputMap.put(node, nextInput);
  }

  protected Set<CFGNode> getInNodes(CFGNode node) {
    return node.getInNodes();
  }

  protected Set<CFGNode> getOutNodes(CFGNode node) {
    return node.getOutNodes();
  }

  /** run analysis and remove redundant temporaries. */
  public StmtNode run(StmtNode program) {
    CFGVisitor cfgVisitor = new CFGVisitor();
    ControlFlowGraph cfg = cfgVisitor.createCFG(program);
    List<CFGNode> nodes = cfg.getNodes();
    dataflow(nodes);

    Queue<CopyPropInfo> inInfoQueue = new LinkedList<>();
    Queue<CopyPropInfo> outInfoQueue = new LinkedList<>();
    for (CFGNode node : nodes) {
      inInfoQueue.add(this.inputMap.get(node));
      outInfoQueue.add(this.outputMap.get(node));
    }

    CopyPropVisitor cp = new CopyPropVisitor(cfg, inInfoQueue, outInfoQueue);
    StmtNode newProgram = cp.propagateCopies(program);
    return newProgram;
  }

  @Override
  public void dataflow(List<CFGNode> nodes) {
    this.inputMap = new HashMap<>();
    this.outputMap = new HashMap<>();
    super.dataflow(nodes);
  }

  static class CopyPropVisitor extends IdentityVisitor {
    final ControlFlowGraph cfg;
    final Queue<CopyPropInfo> inInfoQueue;
    final Queue<CopyPropInfo> outInfoQueue;

    public CopyPropVisitor(ControlFlowGraph cfg,
        Queue<CopyPropInfo> iniq, Queue<CopyPropInfo> outiq) {

      this.cfg = cfg;
      this.inInfoQueue = iniq;
      this.outInfoQueue = outiq;
    }

    public StmtNode propagateCopies(StmtNode program) {
      return program.accept(this);
    }

    @Override
    public StmtNode visit(DeclarationNode declNode) {
      this.inInfoQueue.remove();
      this.outInfoQueue.remove();
      return super.visit(declNode);
    }

    @Override
    public StmtNode visit(ArrayDeclarationNode arrayDeclNode) {
      this.inInfoQueue.remove();
      this.outInfoQueue.remove();
      return super.visit(arrayDeclNode);
    }

    @Override
    public StmtNode visit(AssignNode assignNode) {
      CopyPropInfo inInfo = this.inInfoQueue.remove();
      CopyPropInfo outInfo = this.outInfoQueue.remove();

      Map<Variable,Variable> inRenameMap = processCopyPropInfo(inInfo);
      Map<Variable,Variable> outRenameMap = processCopyPropInfo(outInfo);
      Set<Variable> renamedVars = outRenameMap.keySet();

      // one of the variables to be erased; remove it
      if (renamedVars.contains(assignNode.getVariable())) {
        return new BlockNode();

      // otherwise, rename all variables in the assignment
      } else {
        RenameVisitor renamer = new RenameVisitor(inRenameMap);
        return assignNode.accept(renamer);
      }
    }

    @Override
    public StmtNode visit(IfNode ifNode) {
      CopyPropInfo info = this.inInfoQueue.remove();
      this.outInfoQueue.remove();
      Map<Variable,Variable> renameMap = processCopyPropInfo(info);
      RenameVisitor renamer = new RenameVisitor(renameMap);

      ExpressionNode newGuard = ifNode.getGuard().accept(renamer);
      StmtNode newThenBranch = ifNode.getThenBranch().accept(this);
      StmtNode newElseBranch = ifNode.getElseBranch().accept(this);

      return new IfNode(newGuard, newThenBranch, newElseBranch);
    }

    @Override
    public StmtNode visit(BlockNode block) {
      List<StmtNode> newStmts = new ArrayList<>();

      for (StmtNode stmt : block) {
        StmtNode newStmt = stmt.accept(this);
        if (!(newStmt instanceof BlockNode && ((BlockNode)newStmt).size() == 0)) {
          newStmts.add(newStmt);
        }
      }

      return new BlockNode(newStmts);
    }

    @Override
    public StmtNode visit(SendNode sendNode) {
      this.inInfoQueue.remove();
      this.outInfoQueue.remove();
      return super.visit(sendNode);
    }

    @Override
    public StmtNode visit(ReceiveNode recvNode) {
      this.inInfoQueue.remove();
      this.outInfoQueue.remove();
      return super.visit(recvNode);
    }

    @Override
    public StmtNode visit(AssertNode assertNode) {
      this.inInfoQueue.remove();
      this.outInfoQueue.remove();
      return super.visit(assertNode);
    }

    /** return rename map of variables. */
    private Map<Variable,Variable> processCopyPropInfo(CopyPropInfo info) {
      final List<Variable> vars = this.cfg.getVars();
      final Set<VarEquals> equalities = info.getEqualities();
      List<Set<Variable>> eqSets = new ArrayList<>();

      // compute equivalence class of variables
      for (VarEquals eq : equalities) {
        Variable var1 = eq.getVar1();
        Variable var2 = eq.getVar2();
        int ind1 = -1;
        int ind2 = -1;

        for (int i = 0; i < eqSets.size(); i++) {
          Set<Variable> eqSet = eqSets.get(i);
          if (eqSet.contains(var1)) {
            ind1 = i;
          }
          if (eqSet.contains(var2)) {
            ind2 = i;
          }
        }

        // both vars are already in the same set; do nothing
        if (ind1 == ind2 && ind1 >= 0 && ind2 >= 0) {

        // vars are in different sets; merge them
        } else if (ind1 != ind2 && ind1 >= 0 && ind2 >= 0) {
          Set<Variable> mergeSet1 = eqSets.get(ind1);
          Set<Variable> mergeSet2 = eqSets.get(ind2);
          eqSets.remove(mergeSet1);
          eqSets.remove(mergeSet2);
          mergeSet1.addAll(mergeSet2);
          eqSets.add(mergeSet1);

        // add var1 to var2's set
        } else if (ind1 < 0 && ind2 >= 0) {
          Set<Variable> mergeSet2 = eqSets.get(ind2);
          mergeSet2.add(var1);

        // add var2 to var1's set
        } else if (ind1 >= 0 && ind2 < 0) {
          Set<Variable> mergeSet1 = eqSets.get(ind1);
          mergeSet1.add(var2);

        // both vars are not seen before; add new set containing both
        } else if (ind1 < 0 && ind2 < 0) {
          Set<Variable> mergeSet = new HashSet<>();
          mergeSet.add(var1);
          mergeSet.add(var2);
          eqSets.add(mergeSet);
        }
      }

      // find representatives for each equivalence class
      // representative is earliest variable assigned/declared
      Map<Variable,Set<Variable>> repMap = new HashMap<>();
      for (Set<Variable> eqSet : eqSets) {
        int repInd = -1;
        for (Variable var : eqSet) {
          int varInd = vars.indexOf(var);

          if (repInd > varInd || repInd < 0) {
            repInd = varInd;
          }
        }

        Variable repVar = vars.get(repInd);
        repMap.put(repVar, eqSet);
      }

      // build rename map from rep map
      Map<Variable,Variable> renameMap = new HashMap<>();
      for (Map.Entry<Variable,Set<Variable>> kv : repMap.entrySet()) {
        Variable repVar = kv.getKey();
        for (Variable var : kv.getValue()) {
          if (!var.equals(repVar)) {
            renameMap.put(var, repVar);
          }
        }
      }

      return renameMap;
    }
  }

  static class VarEquals {
    private final Variable var1;
    private final Variable var2;

    public VarEquals(Variable v1, Variable v2) {
      this.var1 = v1;
      this.var2 = v2;
    }

    public Variable getVar1() {
      return this.var1;
    }

    public Variable getVar2() {
      return this.var2;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }

      if (!(o instanceof VarEquals)) {
        return false;
      }

      final VarEquals that = (VarEquals) o;
      return Objects.equals(this.var1, that.var1) && Objects.equals(this.var2, that.var2);
    }

    @Override
    public int hashCode() {
      return Objects.hash(this.var1, this.var2);
    }

    @Override
    public String toString() {
      return String.format("%s=%s", this.var1, this.var2);
    }
  }

  static class CopyPropInfo implements Lattice<CopyPropInfo> {
    Set<VarEquals> equalities;

    public CopyPropInfo() {
      this.equalities = new HashSet<>();
    }

    public CopyPropInfo(Set<VarEquals> es) {
      this.equalities = es;
    }

    public Set<VarEquals> getEqualities() {
      return this.equalities;
    }

    public CopyPropInfo removeVar(Variable v) {
      Set<VarEquals> es = new HashSet<>(this.equalities);
      for (VarEquals eq : this.equalities) {
        if (eq.getVar1().equals(v) || eq.getVar2().equals(v)) {
          es.remove(eq);
        }
      }
      return new CopyPropInfo(es);
    }

    public CopyPropInfo add(Variable v1, Variable v2) {
      Set<VarEquals> es = new HashSet<>(this.equalities);
      VarEquals ve = new VarEquals(v1, v2);
      es.add(ve);
      return new CopyPropInfo(es);
    }

    public CopyPropInfo kill(CFGNode node) {
      StmtNode stmt = node.getStatement();
      if (stmt instanceof AssignNode) {
        AssignNode assignNode = (AssignNode) stmt;
        return this.removeVar(assignNode.getVariable());

      } else if (stmt instanceof ReceiveNode) {
        ReceiveNode recvNode = (ReceiveNode) stmt;
        return this.removeVar(recvNode.getVariable());

      } else {
        return this;
      }
    }

    public CopyPropInfo gen(CFGNode node) {
      StmtNode stmt = node.getStatement();
      if (stmt instanceof AssignNode) {
        AssignNode assignNode = (AssignNode) stmt;
        ExpressionNode rhs = assignNode.getRhs();
        if (rhs instanceof ReadNode) {
          ReadNode rhsRead = (ReadNode) rhs;
          return this.add(assignNode.getVariable(), rhsRead.getVariable());
        }
      }

      return this;
    }

    @Override
    public boolean lessThanOrEqualTo(CopyPropInfo other) {
      return other.equalities.containsAll(this.equalities);
    }

    @Override
    public CopyPropInfo join(CopyPropInfo that) {
      Set<VarEquals> es = new HashSet<>(this.equalities);
      es.addAll(that.equalities);
      return new CopyPropInfo(es);
    }

    @Override
    public CopyPropInfo meet(CopyPropInfo that) {
      Set<VarEquals> es = new HashSet<>(this.equalities);
      es.retainAll(that.equalities);
      return new CopyPropInfo(es);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }

      if (!(o instanceof CopyPropInfo)) {
        return false;
      }

      final CopyPropInfo that = (CopyPropInfo) o;
      return Objects.equals(this.equalities, that.equalities);
    }

    @Override
    public int hashCode() {
      return this.equalities.hashCode();
    }

    @Override
    public String toString() {
      Set<String> strEqualities = new HashSet<>();
      for (VarEquals eq : this.equalities) {
        strEqualities.add(eq.toString());
      }
      String body = String.join(", ", strEqualities);
      return String.format("{%s}", body);
    }
  }
}
