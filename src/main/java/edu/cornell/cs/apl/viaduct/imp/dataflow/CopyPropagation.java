package edu.cornell.cs.apl.viaduct.imp.dataflow;

import edu.cornell.cs.apl.viaduct.dataflow.Dataflow;
import edu.cornell.cs.apl.viaduct.imp.ast.ArrayDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssertNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssignNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.IfNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpValue;
import edu.cornell.cs.apl.viaduct.imp.ast.LetBindingNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LiteralNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReadNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReceiveNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Reference;
import edu.cornell.cs.apl.viaduct.imp.ast.SendNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.imp.ast.VariableDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.visitors.IdentityVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.ReplaceVisitor;
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
  private Map<CFGNode, CopyPropInfo> inputMap;
  private Map<CFGNode, CopyPropInfo> outputMap;

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

  @Override
  protected CopyPropInfo transfer(CFGNode node, CopyPropInfo nextInput) {
    return nextInput.kill(node).gen(node);
  }

  @Override
  protected void updateInput(CFGNode node, CopyPropInfo nextInput) {
    this.inputMap.put(node, nextInput);
  }

  @Override
  protected void updateOutput(CFGNode node, CopyPropInfo nextInput) {
    this.outputMap.put(node, nextInput);
  }

  @Override
  protected Set<CFGNode> getInNodes(CFGNode node) {
    return node.getInNodes();
  }

  @Override
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

    public CopyPropVisitor(
        ControlFlowGraph cfg, Queue<CopyPropInfo> iniq, Queue<CopyPropInfo> outiq) {

      this.cfg = cfg;
      this.inInfoQueue = iniq;
      this.outInfoQueue = outiq;
    }

    public StmtNode propagateCopies(StmtNode program) {
      return program.accept(this);
    }

    @Override
    public StmtNode visit(VariableDeclarationNode declNode) {
      this.inInfoQueue.remove();
      this.outInfoQueue.remove();
      return super.visit(declNode);
    }

    @Override
    public StmtNode visit(ArrayDeclarationNode arrayDeclNode) {
      CopyPropInfo inInfo = this.inInfoQueue.remove();
      this.outInfoQueue.remove();
      return rename(arrayDeclNode, inInfo);
    }

    @Override
    public StmtNode visit(LetBindingNode letBindingNode) {
      CopyPropInfo inInfo = this.inInfoQueue.remove();
      CopyPropInfo outInfo = this.outInfoQueue.remove();

      Map<Variable, ExpressionNode> outRenameMap = processCopyPropInfo(outInfo);
      Set<Variable> renamedVars = outRenameMap.keySet();

      // this is one of the variables to be erased; remove it
      Variable var = letBindingNode.getVariable();
      if (renamedVars.contains(var)) {
        return new BlockNode();
      }

      // otherwise, rename all variables in the assignment
      return rename(letBindingNode, inInfo);
    }

    @Override
    public StmtNode visit(AssignNode assignNode) {
      CopyPropInfo inInfo = this.inInfoQueue.remove();
      CopyPropInfo outInfo = this.outInfoQueue.remove();

      Map<Variable, ExpressionNode> outRenameMap = processCopyPropInfo(outInfo);
      Set<Variable> renamedVars = outRenameMap.keySet();

      // one of the variables to be erased; remove it
      Reference lhs = assignNode.getLhs();
      if (lhs instanceof Variable) {
        Variable var = (Variable) lhs;
        if (renamedVars.contains(var)) {
          return new BlockNode();
        }
      }

      // otherwise, rename all variables in the assignment
      return rename(assignNode, inInfo);
    }

    @Override
    public StmtNode visit(IfNode ifNode) {
      CopyPropInfo info = this.inInfoQueue.remove();
      this.outInfoQueue.remove();

      ExpressionNode newGuard = rename(ifNode.getGuard(), info);
      StmtNode newThenBranch = ifNode.getThenBranch().accept(this);
      StmtNode newElseBranch = ifNode.getElseBranch().accept(this);

      return new IfNode(newGuard, newThenBranch, newElseBranch);
    }

    @Override
    public StmtNode visit(BlockNode block) {
      List<StmtNode> newStmts = new ArrayList<>();

      for (StmtNode stmt : block) {
        StmtNode newStmt = stmt.accept(this);
        if (!(newStmt instanceof BlockNode && ((BlockNode) newStmt).size() == 0)) {
          newStmts.add(newStmt);
        }
      }

      return new BlockNode(newStmts);
    }

    @Override
    public StmtNode visit(SendNode sendNode) {
      CopyPropInfo inInfo = this.inInfoQueue.remove();
      this.outInfoQueue.remove();
      return rename(sendNode, inInfo);
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

    private StmtNode rename(StmtNode stmt, CopyPropInfo info) {
      Map<Variable, ExpressionNode> renameMap = processCopyPropInfo(info);
      ReplaceVisitor renamer = new ReplaceVisitor(renameMap);
      return stmt.accept(renamer);
    }

    private ExpressionNode rename(ExpressionNode expr, CopyPropInfo info) {
      Map<Variable, ExpressionNode> renameMap = processCopyPropInfo(info);
      ReplaceVisitor renamer = new ReplaceVisitor(renameMap);
      return expr.accept(renamer);
    }

    /** return rename map of variables. */
    private Map<Variable, ExpressionNode> processCopyPropInfo(CopyPropInfo info) {
      final List<Variable> vars = this.cfg.getVars();
      List<Set<Variable>> eqSets = new ArrayList<>();

      // compute equivalence class of variables
      for (VarEqualsVar eq : info.getVarEqualities()) {
        Variable var1 = eq.getVar();
        Variable var2 = eq.getRhs();
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
      Map<Variable, Set<Variable>> repMap = new HashMap<>();
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
      Map<Variable, ExpressionNode> replaceMap = new HashMap<>();
      for (Map.Entry<Variable, Set<Variable>> kv : repMap.entrySet()) {
        Variable repVar = kv.getKey();
        for (Variable var : kv.getValue()) {
          if (!var.equals(repVar)) {
            replaceMap.put(var, new ReadNode(repVar));
          }
        }
      }

      // propagate constants as well
      for (VarEqualsVal valEq : info.getValEqualities()) {
        replaceMap.put(valEq.getVar(), new LiteralNode(valEq.getRhs()));
      }

      return replaceMap;
    }
  }

  abstract static class VarEquals<T> {
    private final Variable var;
    private final T rhs;

    public VarEquals(Variable v, T rhs) {
      this.var = v;
      this.rhs = rhs;
    }

    public Variable getVar() {
      return this.var;
    }

    public T getRhs() {
      return this.rhs;
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
      return Objects.equals(this.var, that.var) && Objects.equals(this.rhs, that.rhs);
    }

    @Override
    public int hashCode() {
      return Objects.hash(this.var, this.rhs);
    }

    @Override
    public String toString() {
      return String.format("%s=%s", this.var, this.rhs);
    }
  }

  static class VarEqualsVar extends VarEquals<Variable> {
    public VarEqualsVar(Variable v, Variable rhs) {
      super(v, rhs);
    }
  }

  static class VarEqualsVal extends VarEquals<ImpValue> {
    public VarEqualsVal(Variable v, ImpValue rhs) {
      super(v, rhs);
    }
  }

  static class CopyPropInfo implements Lattice<CopyPropInfo> {
    Set<VarEqualsVar> varEqualities;
    Set<VarEqualsVal> valEqualities;

    public CopyPropInfo() {
      this.varEqualities = new HashSet<>();
      this.valEqualities = new HashSet<>();
    }

    public CopyPropInfo(Set<VarEqualsVar> varEqs, Set<VarEqualsVal> valEqs) {
      this.varEqualities = varEqs;
      this.valEqualities = valEqs;
    }

    public Set<VarEqualsVar> getVarEqualities() {
      return this.varEqualities;
    }

    public Set<VarEqualsVal> getValEqualities() {
      return this.valEqualities;
    }

    public CopyPropInfo removeVar(Variable v) {
      Set<VarEqualsVar> varEqs = new HashSet<>(this.varEqualities);
      for (VarEqualsVar eq : this.varEqualities) {
        if (eq.getVar().equals(v) || eq.getRhs().equals(v)) {
          varEqs.remove(eq);
        }
      }

      Set<VarEqualsVal> valEqs = new HashSet<>(this.valEqualities);
      for (VarEqualsVal eq : this.valEqualities) {
        if (eq.getVar().equals(v)) {
          valEqs.remove(eq);
        }
      }

      return new CopyPropInfo(varEqs, valEqs);
    }

    public CopyPropInfo addVar(Variable v1, Variable v2) {
      Set<VarEqualsVar> varEqs = new HashSet<>(this.varEqualities);
      VarEqualsVar ve = new VarEqualsVar(v1, v2);
      varEqs.add(ve);
      return new CopyPropInfo(varEqs, this.valEqualities);
    }

    public CopyPropInfo addVal(Variable var, ImpValue val) {
      Set<VarEqualsVal> valEqs = new HashSet<>(this.valEqualities);
      VarEqualsVal ve = new VarEqualsVal(var, val);
      valEqs.add(ve);
      return new CopyPropInfo(this.varEqualities, valEqs);
    }

    public CopyPropInfo kill(CFGNode node) {
      StmtNode stmt = node.getStatement();
      if (stmt instanceof AssignNode) {
        AssignNode assignNode = (AssignNode) stmt;
        Reference lhs = assignNode.getLhs();

        if (lhs instanceof Variable) {
          Variable var = (Variable) lhs;
          return this.removeVar(var);

        } else {
          return this;
        }

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
        Reference lhs = assignNode.getLhs();
        ExpressionNode rhs = assignNode.getRhs();

        if (lhs instanceof Variable) {
          Variable var = (Variable) lhs;
          // x = y
          if (rhs instanceof ReadNode && ((ReadNode) rhs).getReference() instanceof Variable) {
            Variable rhsVar = (Variable) ((ReadNode) rhs).getReference();
            return addVar(var, rhsVar);

          // x = n
          } else if (rhs instanceof LiteralNode) {
            LiteralNode rhsLit = (LiteralNode) rhs;
            return addVal(var, rhsLit.getValue());
          }
        }
      } else if (stmt instanceof LetBindingNode) {
        LetBindingNode letBindingNode = (LetBindingNode)stmt;
        Variable var = letBindingNode.getVariable();
        ExpressionNode rhs = letBindingNode.getRhs();

        // let x = y
        if (rhs instanceof ReadNode && ((ReadNode) rhs).getReference() instanceof Variable) {
          Variable rhsVar = (Variable) ((ReadNode) rhs).getReference();
          return addVar(var, rhsVar);

        // let x = n
        } else if (rhs instanceof LiteralNode) {
          LiteralNode rhsLit = (LiteralNode) rhs;
          return addVal(var, rhsLit.getValue());
        }
      }

      return this;
    }

    @Override
    public boolean lessThanOrEqualTo(CopyPropInfo other) {
      return other.varEqualities.containsAll(this.varEqualities);
    }

    @Override
    public CopyPropInfo join(CopyPropInfo that) {
      Set<VarEqualsVar> varEqs = new HashSet<>(this.varEqualities);
      varEqs.addAll(that.varEqualities);

      Set<VarEqualsVal> valEqs = new HashSet<>(this.valEqualities);
      valEqs.addAll(that.valEqualities);

      return new CopyPropInfo(varEqs, valEqs);
    }

    @Override
    public CopyPropInfo meet(CopyPropInfo that) {
      Set<VarEqualsVar> varEqs = new HashSet<>(this.varEqualities);
      varEqs.retainAll(that.varEqualities);

      Set<VarEqualsVal> valEqs = new HashSet<>(this.valEqualities);
      valEqs.retainAll(that.valEqualities);

      return new CopyPropInfo(varEqs, valEqs);
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
      return Objects.equals(this.varEqualities, that.varEqualities)
          && Objects.equals(this.valEqualities, that.valEqualities);
    }

    @Override
    public int hashCode() {
      return Objects.hash(this.varEqualities.hashCode(), this.valEqualities.hashCode());
    }

    @Override
    public String toString() {
      Set<String> strVarEqs = new HashSet<>();
      for (VarEqualsVar eq : this.varEqualities) {
        strVarEqs.add(eq.toString());
      }
      String varBody = String.join(", ", strVarEqs);

      Set<String> strValEqs = new HashSet<>();
      for (VarEqualsVal eq : this.valEqualities) {
        strValEqs.add(eq.toString());
      }
      String valBody = String.join(", ", strValEqs);

      return String.format("{%s} {%s}", varBody, valBody);
    }
  }
}
