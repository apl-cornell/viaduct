package edu.cornell.cs.apl.viaduct.imp.informationflow;

import edu.cornell.cs.apl.viaduct.imp.ElaborationException;
import edu.cornell.cs.apl.viaduct.imp.VariableContext;
import edu.cornell.cs.apl.viaduct.imp.ast.ArrayDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ArrayIndex;
import edu.cornell.cs.apl.viaduct.imp.ast.AssertNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssignNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BinaryExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BreakNode;
import edu.cornell.cs.apl.viaduct.imp.ast.DowngradeNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ForNode;
import edu.cornell.cs.apl.viaduct.imp.ast.IfNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LetBindingNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LiteralNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LoopNode;
import edu.cornell.cs.apl.viaduct.imp.ast.NotNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReadNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReceiveNode;
import edu.cornell.cs.apl.viaduct.imp.ast.SendNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StatementNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.imp.ast.VariableDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.WhileNode;
import edu.cornell.cs.apl.viaduct.imp.visitors.ExprVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.PrintVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.ReferenceVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;
import edu.cornell.cs.apl.viaduct.security.FreeDistributiveLattice;
import edu.cornell.cs.apl.viaduct.security.Label;
import edu.cornell.cs.apl.viaduct.security.Principal;
import edu.cornell.cs.apl.viaduct.security.solver.ConstantTerm;
import edu.cornell.cs.apl.viaduct.security.solver.ConstraintSystem;
import edu.cornell.cs.apl.viaduct.security.solver.ConstraintValue;
import edu.cornell.cs.apl.viaduct.security.solver.VariableTerm;
import edu.cornell.cs.apl.viaduct.util.FreshNameGenerator;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

/**
 * Check that all flows of information within the program are safe.
 *
 * <p>Additionally, adds a minimum-trust label to each node in the AST. This label indicates the
 * minimum amount of trust the host executing that node needs to have for the execution to be
 * secure.
 */
public class InformationFlowChecker
    implements ReferenceVisitor<LabelTerm>, ExprVisitor<LabelTerm>, StmtVisitor<Void> {

  /** Variable declarations that are in scope. */
  private final VariableContext<LabelTerm> declarations;

  private final ConstraintSystem<FreeDistributiveLattice<Principal>> constraintSystem;

  private final FreshNameGenerator nameGenerator;

  /**
   * Solutions for all variables are added to this map at the end. This is then used by {@link
   * LabelVariable#getValue()}.
   */
  private final Map<
          VariableTerm<FreeDistributiveLattice<Principal>>, FreeDistributiveLattice<Principal>>
      solutions = new HashMap<>();

  /** Current program counter label. */
  private LabelVariable pc;

  private LabelVariable breakLabel;

  /** constructor. create fresh PC and break labels. */
  public InformationFlowChecker() {
    this.declarations = new VariableContext<>();
    this.constraintSystem = new ConstraintSystem<>(FreeDistributiveLattice.top());
    this.nameGenerator = new FreshNameGenerator();
    this.pc = new LabelVariable(this.nameGenerator.getFreshName("pc"));
    this.breakLabel = new LabelVariable(this.nameGenerator.getFreshName("break"));
  }

  /** Check and decorate an expression. */
  public void run(ExpressionNode expr) {
    expr.accept(this);
    this.solutions.putAll(this.constraintSystem.solve());
  }

  /** Check and decorate a statement. */
  public void run(StatementNode stmt) {
    stmt.accept(this);
    this.solutions.putAll(this.constraintSystem.solve());
  }

  //  /** Check and decorate a program. */
  //  public static void run(ProgramNode program) throws UnsatisfiableConstraintException {
  //    final InformationFlowChecker checker = new InformationFlowChecker();
  //    program.accept(checker);
  //    checker.solutions.putAll(checker.constraintSystem.solve());
  //  }

  /**
   * Add constraints to {@link #constraintSystem} that are equivalent to {@code lhs.flowsTo(rhs)}.
   */
  private void addFlowsToConstraint(LabelTerm lhs, LabelTerm rhs) {
    // NOTE: it is very important these constraints mirror those in {@link Label#flowsTo(Label)}.
    constraintSystem.addLessThanOrEqualToConstraint(
        rhs.getConfidentiality(), lhs.getConfidentiality());
    constraintSystem.addLessThanOrEqualToConstraint(lhs.getIntegrity(), rhs.getIntegrity());
  }

  /**
   * Add constraints to {@link #constraintSystem} that assert {@code l1} and {@code l2} are the same
   * label.
   */
  private void addEqualToConstraint(LabelTerm l1, LabelTerm l2) {
    addFlowsToConstraint(l1, l2);
    addFlowsToConstraint(l2, l1);
  }

  private LabelConstant createLabelConstant(Label label) {
    ConstantTerm<FreeDistributiveLattice<Principal>> confidentiality =
        this.constraintSystem.addNewConstant(label.confidentialityComponent());
    ConstantTerm<FreeDistributiveLattice<Principal>> integrity =
        this.constraintSystem.addNewConstant(label.integrityComponent());
    return new LabelConstant(confidentiality, integrity);
  }

  public void exportDotGraph(Writer writer) {
    this.constraintSystem.exportDotGraph(this.solutions, writer);
  }

  @Override
  public LabelTerm visit(Variable variable) {
    final LabelTerm l = declarations.get(variable);

    // We leak the pc every time we read or write a variable.
    // TODO: need to distinguish b/w let-binding variables and assignables
    addFlowsToConstraint(pc, l);

    return l;
  }

  @Override
  public LabelTerm visit(ArrayIndex arrayIndex) {
    final LabelTerm l = declarations.get(arrayIndex.getArray());

    // We leak the pc every time we read or write a variable.
    addFlowsToConstraint(pc, l);

    // Same with the index we are looking up.
    final LabelTerm e = arrayIndex.getIndex().accept(this);
    addFlowsToConstraint(e, l);

    return l;
  }

  @Override
  public LabelTerm visit(LiteralNode literalNode) {
    final LabelVariable l =
        new LabelVariable(
            this.nameGenerator.getFreshName("lit"), literalNode.getValue().toString());
    literalNode.setTrustLabel(l);
    return l;
  }

  @Override
  public LabelTerm visit(ReadNode readNode) {
    final LabelTerm l = readNode.getReference().accept(this);
    readNode.setTrustLabel(l);
    return l;
  }

  @Override
  public LabelTerm visit(NotNode notNode) {
    final LabelVariable l =
        new LabelVariable(this.nameGenerator.getFreshName("not"), PrintVisitor.run(notNode));
    notNode.setTrustLabel(l);

    final LabelTerm expLabel = notNode.getExpression().accept(this);
    addFlowsToConstraint(expLabel, l);

    return l;
  }

  @Override
  public LabelTerm visit(BinaryExpressionNode binExprNode) {
    final LabelVariable l =
        new LabelVariable(this.nameGenerator.getFreshName("binop"), PrintVisitor.run(binExprNode));
    binExprNode.setTrustLabel(l);

    final LabelTerm lhsLabel = binExprNode.getLhs().accept(this);
    final LabelTerm rhsLabel = binExprNode.getRhs().accept(this);
    addFlowsToConstraint(lhsLabel, l);
    addFlowsToConstraint(rhsLabel, l);

    return l;
  }

  @Override
  public LabelTerm visit(DowngradeNode downgradeNode) {
    final LabelConstant toLabel = createLabelConstant(downgradeNode.getToLabel());
    downgradeNode.setTrustLabel(toLabel);

    // pc is leaked to the output label
    addFlowsToConstraint(pc, toLabel);

    // Non-malleable downgrade constraints
    final LabelTerm e = downgradeNode.getExpression().accept(this);
    final ConstraintValue<FreeDistributiveLattice<Principal>> ec = e.getConfidentiality();
    final ConstraintValue<FreeDistributiveLattice<Principal>> ei = e.getIntegrity();
    final ConstraintValue<FreeDistributiveLattice<Principal>> pcc = pc.getConfidentiality();
    final ConstraintValue<FreeDistributiveLattice<Principal>> pci = pc.getIntegrity();
    final ConstantTerm<FreeDistributiveLattice<Principal>> dc = toLabel.getConfidentiality();
    final ConstantTerm<FreeDistributiveLattice<Principal>> di = toLabel.getIntegrity();

    constraintSystem.addLessThanOrEqualToConstraint(dc.meet(pci), ec);
    constraintSystem.addLessThanOrEqualToConstraint(dc.meet(ei), ec);
    constraintSystem.addLessThanOrEqualToConstraint(ei, di.join(pcc));
    constraintSystem.addLessThanOrEqualToConstraint(ei, di.join(ec));

    // if there is a from label, then it has to equal e
    final Label fromLabel = downgradeNode.getFromLabel();
    if (fromLabel != null) {
      final LabelTerm fromLabelTerm = createLabelConstant(fromLabel);
      addEqualToConstraint(fromLabelTerm, e);

    } else { // no from label, but we want to enforce single-dimensional downgrades also
      switch (downgradeNode.getDowngradeType()) {
        case ENDORSE: // confidentiality must remain the same
          constraintSystem.addLessThanOrEqualToConstraint(ec, dc);
          constraintSystem.addLessThanOrEqualToConstraint(dc, ec);
          break;

        case DECLASSIFY: // integrity must remain the same
          constraintSystem.addLessThanOrEqualToConstraint(ei, di);
          constraintSystem.addLessThanOrEqualToConstraint(di, ei);
          break;

        case BOTH: // no extra constraints for simultaneous downgrades
        default:
          break;
      }
    }

    return toLabel;
  }

  @Override
  public Void visit(VariableDeclarationNode varDeclNode) {
    final Label varLabel = varDeclNode.getLabel();

    LabelTerm l;
    if (varLabel != null) {
      l = createLabelConstant(varDeclNode.getLabel());

    } else {
      l = new LabelVariable(varDeclNode.getVariable().toString());
    }

    varDeclNode.setTrustLabel(l);
    declarations.put(varDeclNode.getVariable(), l);

    return null;
  }

  @Override
  public Void visit(ArrayDeclarationNode arrayDeclNode) {
    final Label arrayLabel = arrayDeclNode.getLabel();

    LabelTerm l;
    if (arrayLabel != null) {
      l = createLabelConstant(arrayDeclNode.getLabel());

    } else {
      l = new LabelVariable(arrayDeclNode.getVariable().toString());
    }

    arrayDeclNode.setTrustLabel(l);
    final LabelTerm e = arrayDeclNode.getLength().accept(this);
    addFlowsToConstraint(e, l);

    declarations.put(arrayDeclNode.getVariable(), l);

    return null;
  }

  @Override
  public Void visit(LetBindingNode letBindingNode) {
    final LabelVariable l = new LabelVariable(letBindingNode.getVariable().toString());
    letBindingNode.setTrustLabel(l);

    final LabelTerm e = letBindingNode.getRhs().accept(this);
    addFlowsToConstraint(e, l);

    declarations.put(letBindingNode.getVariable(), l);

    return null;
  }

  @Override
  public Void visit(AssignNode assignNode) {
    final LabelVariable l =
        new LabelVariable(this.nameGenerator.getFreshName("assign"), PrintVisitor.run(assignNode));
    assignNode.setTrustLabel(l);

    final LabelTerm rhsLabel = assignNode.getRhs().accept(this);
    addFlowsToConstraint(this.pc, l);
    addFlowsToConstraint(rhsLabel, l);
    addFlowsToConstraint(l, assignNode.getLhs().accept(this));

    return null;
  }

  @Override
  public Void visit(SendNode sendNode) {
    // TODO: we need global types to do this properly.

    // At least we can check that the expression is sensible.
    sendNode.getSentExpression().accept(this);

    return null;
  }

  @Override
  public Void visit(ReceiveNode receiveNode) {
    // TODO: we need global types to do this properly.

    // At least we can check that the variable access is sensible.
    receiveNode.getVariable().accept(this);

    return null;
  }

  @Override
  public Void visit(IfNode ifNode) {
    final LabelTerm guard = ifNode.getGuard().accept(this);
    ifNode.setTrustLabel(guard);

    // Save old pc
    final LabelVariable oldPc = this.pc;

    // Update pc to include guard
    this.pc = new LabelVariable(this.nameGenerator.getFreshName("pc_if"));
    addFlowsToConstraint(oldPc, this.pc);
    addFlowsToConstraint(guard, this.pc);

    // Check branches
    ifNode.getThenBranch().accept(this);
    ifNode.getElseBranch().accept(this);

    // Recover old pc
    this.pc = oldPc;

    return null;
  }

  @Override
  public Void visit(WhileNode whileNode) {
    throw new ElaborationException();
  }

  @Override
  public Void visit(ForNode forNode) {
    throw new ElaborationException();
  }

  @Override
  public Void visit(LoopNode loopNode) {
    final LabelVariable oldBreakLabel = this.breakLabel;
    final LabelVariable oldPc = this.pc;

    final LabelVariable l = new LabelVariable(this.nameGenerator.getFreshName("break"));
    this.breakLabel = l;
    this.pc = l;

    addFlowsToConstraint(oldPc, this.breakLabel);
    loopNode.setTrustLabel(this.breakLabel);
    loopNode.getBody().accept(this);

    this.breakLabel = oldBreakLabel;
    this.pc = oldPc;
    return null;
  }

  @Override
  public Void visit(BreakNode breakNode) {
    breakNode.setTrustLabel(this.pc);
    addFlowsToConstraint(this.pc, this.breakLabel);
    return null;
  }

  @Override
  public Void visit(BlockNode blockNode) {
    blockNode.setTrustLabel(this.pc);

    declarations.push();
    for (StatementNode stmt : blockNode) {
      stmt.accept(this);
    }
    declarations.pop();

    return null;
  }

  @Override
  public Void visit(AssertNode assertNode) {
    final LabelVariable l = new LabelVariable(this.nameGenerator.getFreshName("assert"));
    assertNode.setTrustLabel(l);
    return null;
  }

  // TODO: add visit(ProgramNode)?

  /** A variable that will be solved for. */
  private final class LabelVariable extends LabelTerm {
    private final VariableTerm<FreeDistributiveLattice<Principal>> confidentiality;
    private final VariableTerm<FreeDistributiveLattice<Principal>> integrity;

    LabelVariable(String id) {
      this.confidentiality = constraintSystem.addNewVariable(String.format("conf_%s", id));
      this.integrity = constraintSystem.addNewVariable(String.format("integ_%s", id));
    }

    LabelVariable(String id, String label) {
      this.confidentiality =
          constraintSystem.addNewVariable(
              String.format("conf_%s", id), String.format("conf_%s", label));
      this.integrity =
          constraintSystem.addNewVariable(
              String.format("integ_%s", id), String.format("integ_%s", label));
    }

    @Override
    public Label getValue() {
      FreeDistributiveLattice<Principal> c = solutions.get(confidentiality);
      FreeDistributiveLattice<Principal> i = solutions.get(integrity);
      // Note: this will fail if either component is null.
      return new Label(c, i);
    }

    @Override
    public VariableTerm<FreeDistributiveLattice<Principal>> getConfidentiality() {
      return confidentiality;
    }

    @Override
    public VariableTerm<FreeDistributiveLattice<Principal>> getIntegrity() {
      return integrity;
    }
  }
}
