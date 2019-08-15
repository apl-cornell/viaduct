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
import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.imp.ast.VariableDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.WhileNode;
import edu.cornell.cs.apl.viaduct.imp.visitors.ExprVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.ReferenceVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;
import edu.cornell.cs.apl.viaduct.security.FreeDistributiveLattice;
import edu.cornell.cs.apl.viaduct.security.Label;
import edu.cornell.cs.apl.viaduct.security.Principal;
import edu.cornell.cs.apl.viaduct.security.solver.ConstantTerm;
import edu.cornell.cs.apl.viaduct.security.solver.ConstraintSystem;
import edu.cornell.cs.apl.viaduct.security.solver.ConstraintValue;
import edu.cornell.cs.apl.viaduct.security.solver.UnsatisfiableConstraintException;
import edu.cornell.cs.apl.viaduct.security.solver.VariableTerm;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Check that all flows of information within the program are safe.
 *
 * <p>Additionally, adds a minimum-trust label to each node in the AST. This label indicates the
 * minimum amount of trust the host executing that node needs to have for the execution to be
 * secure.
 */
public class InformationFlowChecker
    implements ReferenceVisitor<LabelTerm>, ExprVisitor<LabelTerm>, StmtVisitor<LabelTerm> {

  /** Variable declarations that are in scope. */
  private final VariableContext<LabelTerm> declarations = new VariableContext<>();

  private final ConstraintSystem<FreeDistributiveLattice<Principal>> constraintSystem =
      new ConstraintSystem<>(FreeDistributiveLattice.top());

  /**
   * Solutions for all variables are added to this map at the end. This is then used by {@link
   * LabelVariable#getValue()}.
   */
  private final Map<VariableTerm, FreeDistributiveLattice<Principal>> solutions = new HashMap<>();

  /** Current program counter label. */
  private LabelVariable pc = new LabelVariable();

  private InformationFlowChecker() {}

  /** Check and decorate an expression. */
  public static void run(ExpressionNode expression) throws UnsatisfiableConstraintException {
    final InformationFlowChecker checker = new InformationFlowChecker();
    expression.accept(checker);
    checker.solutions.putAll(checker.constraintSystem.solve());
  }

  /** Check and decorate a statement. */
  public static void run(StmtNode statement) throws UnsatisfiableConstraintException {
    final InformationFlowChecker checker = new InformationFlowChecker();
    statement.accept(checker);
    checker.solutions.putAll(checker.constraintSystem.solve());
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

  private LabelConstant createLabelConstant(Label label) {
    ConstantTerm<FreeDistributiveLattice<Principal>> confidentiality =
        this.constraintSystem.addNewConstant(label.confidentialityComponent());
    ConstantTerm<FreeDistributiveLattice<Principal>> integrity =
        this.constraintSystem.addNewConstant(label.integrityComponent());
    LabelConstant constant = new LabelConstant(confidentiality, integrity);
    return constant;
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
    final LabelVariable l = new LabelVariable();
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
    final LabelVariable l = new LabelVariable();
    notNode.setTrustLabel(l);

    final LabelTerm expLabel = notNode.getExpression().accept(this);
    addFlowsToConstraint(expLabel, l);

    return l;
  }

  @Override
  public LabelTerm visit(BinaryExpressionNode binaryExpressionNode) {
    final LabelVariable l = new LabelVariable();
    binaryExpressionNode.setTrustLabel(l);

    final LabelTerm lhsLabel = binaryExpressionNode.getLhs().accept(this);
    final LabelTerm rhsLabel = binaryExpressionNode.getRhs().accept(this);
    addFlowsToConstraint(lhsLabel, l);
    addFlowsToConstraint(rhsLabel, l);

    return l;
  }

  @Override
  public LabelTerm visit(DowngradeNode downgradeNode) {
    final LabelConstant l = createLabelConstant(downgradeNode.getLabel());
    downgradeNode.setTrustLabel(l);

    // pc is leaked to the output label
    addFlowsToConstraint(pc, l);

    // Non-malleable downgrade constraints
    final LabelTerm e = downgradeNode.getExpression().accept(this);
    final ConstraintValue<FreeDistributiveLattice<Principal>> ec = e.getConfidentiality();
    final ConstraintValue<FreeDistributiveLattice<Principal>> ei = e.getIntegrity();
    final ConstraintValue<FreeDistributiveLattice<Principal>> pcc = pc.getConfidentiality();
    final ConstraintValue<FreeDistributiveLattice<Principal>> pci = pc.getIntegrity();
    final ConstantTerm<FreeDistributiveLattice<Principal>> dc = l.getConfidentiality();
    final ConstantTerm<FreeDistributiveLattice<Principal>> di = l.getIntegrity();

    constraintSystem.addLessThanOrEqualToConstraint(dc.meet(pci), ec);
    constraintSystem.addLessThanOrEqualToConstraint(dc.meet(ei), ec);
    constraintSystem.addLessThanOrEqualToConstraint(ei, di.join(pcc));
    constraintSystem.addLessThanOrEqualToConstraint(ei, di.join(ec));

    return l;
  }

  @Override
  public LabelTerm visit(VariableDeclarationNode variableDeclarationNode) {
    final LabelConstant l = createLabelConstant(variableDeclarationNode.getLabel());
    variableDeclarationNode.setTrustLabel(l);

    declarations.put(variableDeclarationNode.getVariable(), l);

    return l;
  }

  @Override
  public LabelTerm visit(ArrayDeclarationNode arrayDeclarationNode) {
    final LabelConstant l = createLabelConstant(arrayDeclarationNode.getLabel());
    arrayDeclarationNode.setTrustLabel(l);

    final LabelTerm e = arrayDeclarationNode.getLength().accept(this);
    addFlowsToConstraint(e, l);

    declarations.put(arrayDeclarationNode.getVariable(), l);

    return l;
  }

  @Override
  public LabelTerm visit(LetBindingNode letBindingNode) {
    final LabelVariable l = new LabelVariable();
    letBindingNode.setTrustLabel(l);

    final LabelTerm e = letBindingNode.getRhs().accept(this);
    addFlowsToConstraint(e, l);

    declarations.put(letBindingNode.getVariable(), l);

    return l;
  }

  @Override
  public LabelTerm visit(AssignNode assignNode) {
    final LabelVariable l = new LabelVariable();
    assignNode.setTrustLabel(l);

    final LabelTerm rhsLabel = assignNode.getRhs().accept(this);
    addFlowsToConstraint(rhsLabel, l);

    addFlowsToConstraint(l, assignNode.getLhs().accept(this));

    return l;
  }

  @Override
  public LabelTerm visit(SendNode sendNode) {
    // TODO: we need global types to do this properly.

    // At least we can check that the expression is sensible.
    sendNode.getSentExpression().accept(this);

    return null;
  }

  @Override
  public LabelTerm visit(ReceiveNode receiveNode) {
    // TODO: we need global types to do this properly.

    // At least we can check that the variable access is sensible.
    receiveNode.getVariable().accept(this);

    return null;
  }

  @Override
  public LabelTerm visit(IfNode ifNode) {
    final LabelTerm guard = ifNode.getGuard().accept(this);
    ifNode.setTrustLabel(guard);

    // Save old pc
    final LabelVariable oldPc = this.pc;

    // Update pc to include guard
    this.pc = new LabelVariable();
    addFlowsToConstraint(oldPc, pc);
    addFlowsToConstraint(guard, pc);

    // Check branches
    ifNode.getThenBranch().accept(this);
    ifNode.getElseBranch().accept(this);

    // Recover old pc
    this.pc = oldPc;

    return guard;
  }

  @Override
  public LabelTerm visit(WhileNode whileNode) {
    throw new ElaborationException();
  }

  @Override
  public LabelTerm visit(ForNode forNode) {
    throw new ElaborationException();
  }

  @Override
  public LabelTerm visit(LoopNode loopNode) {
    // TODO: need to maintain a stack of new pc labels.
    //   see IfNode for an example of how we change the pc.
    final LabelVariable l = new LabelVariable();
    loopNode.setTrustLabel(l);
    loopNode.getBody().accept(this);
    return l;
  }

  @Override
  public LabelTerm visit(BreakNode breakNode) {
    // TODO: leak pc to the broken loop
    final LabelVariable l = new LabelVariable();
    breakNode.setTrustLabel(l);
    return new LabelVariable();
  }

  @Override
  public LabelTerm visit(BlockNode blockNode) {
    final LabelVariable l = pc;
    blockNode.setTrustLabel(l);

    declarations.push();
    for (StmtNode stmt : blockNode) {
      stmt.accept(this);
    }
    declarations.pop();

    return l;
  }

  @Override
  public LabelTerm visit(AssertNode assertNode) {
    // TODO: Not sure about how much integrity we need and if this affects pc.
    final LabelTerm l = assertNode.getExpression().accept(this);
    assertNode.setTrustLabel(l);

    return l;
  }

  // TODO: add visit(ProgramNode)?

  /** A variable that will be solved for. */
  private final class LabelVariable extends LabelTerm {
    private final VariableTerm<FreeDistributiveLattice<Principal>>
        confidentiality = constraintSystem.addNewVariable();

    private final VariableTerm<FreeDistributiveLattice<Principal>> integrity =
        constraintSystem.addNewVariable();

    @Override
    public Label getValue() {
      FreeDistributiveLattice<Principal> c = Objects.requireNonNull(solutions.get(confidentiality));
      FreeDistributiveLattice<Principal> i = Objects.requireNonNull(solutions.get(integrity));
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
