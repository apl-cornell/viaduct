package edu.cornell.cs.apl.viaduct.imp.informationflow;

import edu.cornell.cs.apl.viaduct.errors.ConfidentialityChangingEndorsementError;
import edu.cornell.cs.apl.viaduct.errors.ElaborationException;
import edu.cornell.cs.apl.viaduct.errors.InformationFlowError;
import edu.cornell.cs.apl.viaduct.errors.InsecureControlFlowError;
import edu.cornell.cs.apl.viaduct.errors.InsecureDataFlowError;
import edu.cornell.cs.apl.viaduct.errors.IntegrityChangingDeclassificationError;
import edu.cornell.cs.apl.viaduct.errors.LabelMismatchError;
import edu.cornell.cs.apl.viaduct.errors.MalleableDowngradeError;
import edu.cornell.cs.apl.viaduct.errors.UndefinedNameError;
import edu.cornell.cs.apl.viaduct.imp.ast.ArrayDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ArrayIndexingNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssertNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssignNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BinaryExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BreakNode;
import edu.cornell.cs.apl.viaduct.imp.ast.DowngradeNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ForNode;
import edu.cornell.cs.apl.viaduct.imp.ast.HostDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.HostName;
import edu.cornell.cs.apl.viaduct.imp.ast.IfNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LetBindingNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LiteralNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LoopNode;
import edu.cornell.cs.apl.viaduct.imp.ast.NotNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReadNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReceiveNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReferenceNode;
import edu.cornell.cs.apl.viaduct.imp.ast.SendNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StatementNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.imp.ast.VariableDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.WhileNode;
import edu.cornell.cs.apl.viaduct.imp.parsing.HasLocation;
import edu.cornell.cs.apl.viaduct.imp.parsing.Printer;
import edu.cornell.cs.apl.viaduct.imp.visitors.AbstractExprVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.AbstractReferenceVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.ExprVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.IdentityProgramVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.LoopContextStmtVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.ReferenceVisitor;
import edu.cornell.cs.apl.viaduct.security.Label;
import edu.cornell.cs.apl.viaduct.security.solver.AtomicLabelTerm;
import edu.cornell.cs.apl.viaduct.security.solver.ConstraintSolver;
import edu.cornell.cs.apl.viaduct.security.solver.LabelConstant;
import edu.cornell.cs.apl.viaduct.security.solver.LabelTerm;
import edu.cornell.cs.apl.viaduct.security.solver.LabelVariable;
import edu.cornell.cs.apl.viaduct.util.FreshNameGenerator;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

final class CheckStmtVisitor
    extends LoopContextStmtVisitor<
        CheckStmtVisitor, AtomicLabelTerm, LabelVariable, AtomicLabelTerm, AtomicLabelTerm, Void> {

  private final CheckReferenceVisitor referenceVisitor = new CheckReferenceVisitor();
  private final CheckExprVisitor expressionVisitor = new CheckExprVisitor();

  private final Map<HostName, HostDeclarationNode> hosts;

  private final ConstraintSolver<InformationFlowError> constraintSystem;

  private final FreshNameGenerator nameGenerator;

  /**
   * Solutions for variables are added to this map at the end. This is then used by {@link
   * LabelTerm#getValue(Map)}.
   */
  private final Map<LabelVariable, Label> solutions;

  /** Current program counter label. */
  private final LabelVariable pc;

  private CheckStmtVisitor(Map<HostName, HostDeclarationNode> hosts) {
    this.hosts = hosts;
    this.constraintSystem = new ConstraintSolver<>();
    this.nameGenerator = new FreshNameGenerator();
    this.solutions = new HashMap<>();
    this.pc = constraintSystem.addNewVariable(nameGenerator.getFreshName("pc"));
  }

  /**
   * Create a new visitor with a different program counter label. All other fields are inherited
   * from {@code visitor}.
   */
  private CheckStmtVisitor(CheckStmtVisitor visitor, LabelVariable pc) {
    super(visitor);
    this.hosts = visitor.hosts;
    this.constraintSystem = visitor.constraintSystem;
    this.nameGenerator = visitor.nameGenerator;
    this.solutions = visitor.solutions;
    this.pc = pc;
  }

  /** Check the given statement node and annotate it with inferred labels. */
  static void run(StatementNode statement, Map<HostName, HostDeclarationNode> hosts) {
    final CheckStmtVisitor visitor = new CheckStmtVisitor(hosts);
    statement.accept(visitor);
    visitor.solutions.putAll(visitor.constraintSystem.solve());
  }

  /** Generate the constraint graph for a statement and write it as a DOT file to {@code output}. */
  static void exportDotGraph(
      StatementNode statement, Map<HostName, HostDeclarationNode> hosts, Writer output) {
    // Copy the statement so we don't overwrite the trust labels in the original.
    final StatementNode freshStatement = new IdentityProgramVisitor().run(statement);
    final CheckStmtVisitor visitor = new CheckStmtVisitor(hosts);
    freshStatement.accept(visitor);
    visitor.constraintSystem.exportDotGraph(output);
  }

  /** Set the trust label of a node to the (future) value of a constraint term. */
  private void setTrustLabel(ImpAstNode node, LabelTerm term) {
    node.setTrustLabel(() -> term.getValue(this.solutions));
  }

  /** Return the declared label of a host. */
  private Label getHostLabel(HostName host) {
    final HostDeclarationNode declaration = hosts.get(host);
    if (declaration != null) {
      return declaration.getTrust();
    } else {
      throw new UndefinedNameError(host);
    }
  }

  /**
   * Create a fresh variable for an AST node and set that node's trust label to the future value of
   * the new variable.
   *
   * @param node AST node that will get its trust label set
   * @return the freshly generated variable
   */
  private LabelVariable setTrustLabelToFreshVariable(ImpAstNode node) {
    final LabelVariable variable = constraintSystem.addNewVariable(new PrettyNodeWrapper(node));
    setTrustLabel(node, variable);
    return variable;
  }

  /** Assert that it is safe for {@code node}'s output to flow to {@code to}. */
  private void addOutputFlowsToConstraint(
      HasLocation node, AtomicLabelTerm nodeLabel, LabelTerm to) {
    constraintSystem.addFlowsToConstraint(
        nodeLabel,
        to,
        (actualNodeLabel, toLabel) -> new InsecureDataFlowError(node, actualNodeLabel, toLabel));
  }

  /** Assert that it is safe for {@code pc} to flow into a node. */
  private void addPcFlowsToConstraint(HasLocation node, AtomicLabelTerm nodeLabel) {
    constraintSystem.addFlowsToConstraint(
        pc,
        nodeLabel,
        (pcLabel, actualNodeLabel) -> new InsecureControlFlowError(node, actualNodeLabel, pcLabel));
  }

  @Override
  protected ReferenceVisitor<AtomicLabelTerm> getReferenceVisitor() {
    return referenceVisitor;
  }

  @Override
  protected ExprVisitor<AtomicLabelTerm> getExpressionVisitor() {
    return expressionVisitor;
  }

  @Override
  protected AtomicLabelTerm extract(VariableDeclarationNode node) {
    if (node.getLabel() != null) {
      final LabelConstant l = LabelConstant.create(node.getLabel());
      setTrustLabel(node, l);
      return l;
    } else {
      return setTrustLabelToFreshVariable(node);
    }
  }

  @Override
  protected AtomicLabelTerm extract(ArrayDeclarationNode node, AtomicLabelTerm length) {
    AtomicLabelTerm l;
    if (node.getLabel() != null) {
      l = LabelConstant.create(node.getLabel());
      setTrustLabel(node, l);
    } else {
      l = setTrustLabelToFreshVariable(node);
    }

    addOutputFlowsToConstraint(node.getLength(), length, l);
    return l;
  }

  @Override
  protected AtomicLabelTerm extract(LetBindingNode node, AtomicLabelTerm rhs) {
    final LabelVariable l = setTrustLabelToFreshVariable(node);
    addOutputFlowsToConstraint(node.getRhs(), rhs, l);
    return l;
  }

  @Override
  protected AtomicLabelTerm extract(ReceiveNode node) {
    return setTrustLabelToFreshVariable(node);
  }

  @Override
  protected LabelVariable extract(WhileNode node) {
    throw new ElaborationException();
  }

  @Override
  protected LabelVariable extract(ForNode node) {
    throw new ElaborationException();
  }

  @Override
  protected LabelVariable extract(LoopNode node) {
    final LabelVariable l = constraintSystem.addNewVariable(nameGenerator.getFreshName("pc"));
    setTrustLabel(node, l);
    addPcFlowsToConstraint(node, l);
    return l;
  }

  @Override
  protected CheckStmtVisitor newScope() {
    return new CheckStmtVisitor(this, pc);
  }

  @Override
  protected CheckStmtVisitor enter(StatementNode node) {
    return this;
  }

  @Override
  protected CheckStmtVisitor enterBody(LoopNode node, LabelVariable newPc) {
    return new CheckStmtVisitor(this, newPc);
  }

  @Override
  protected Void leave(VariableDeclarationNode node, CheckStmtVisitor visitor) {
    return null;
  }

  @Override
  protected Void leave(
      ArrayDeclarationNode node, CheckStmtVisitor visitor, AtomicLabelTerm length) {
    return null;
  }

  @Override
  protected Void leave(LetBindingNode node, CheckStmtVisitor visitor, AtomicLabelTerm rhs) {
    return null;
  }

  @Override
  protected Void leave(
      AssignNode node, CheckStmtVisitor visitor, AtomicLabelTerm lhs, AtomicLabelTerm rhs) {
    final LabelVariable l = setTrustLabelToFreshVariable(node);

    addPcFlowsToConstraint(node.getRhs(), l);
    addOutputFlowsToConstraint(node.getRhs(), rhs, l);
    addOutputFlowsToConstraint(node.getRhs(), l, lhs);
    return null;
  }

  @Override
  protected Void leave(SendNode node, CheckStmtVisitor visitor, AtomicLabelTerm sentExpression) {
    final LabelVariable l = setTrustLabelToFreshVariable(node);

    addPcFlowsToConstraint(node.getSentExpression(), l);
    addOutputFlowsToConstraint(node.getSentExpression(), sentExpression, l);

    if (node.getRecipient().isHost()) {
      final LabelConstant hostLabel =
          LabelConstant.create(getHostLabel(node.getRecipient().toHostName()));

      // Assume the host expects data at its label.
      addOutputFlowsToConstraint(node.getSentExpression(), l, hostLabel);
    }
    return null;
  }

  @Override
  protected Void leave(ReceiveNode node, CheckStmtVisitor visitor, AtomicLabelTerm lhs) {
    final LabelVariable l = setTrustLabelToFreshVariable(node);

    addPcFlowsToConstraint(node.getSender(), l);
    addOutputFlowsToConstraint(node.getSender(), l, lhs);

    if (node.getSender().isHost()) {
      final LabelConstant hostLabel =
          LabelConstant.create(getHostLabel(node.getSender().toHostName()));

      // Assume the received data has the same label as the host.
      addOutputFlowsToConstraint(node.getSender(), hostLabel, l);

      // We leak the pc to the host even when we receive from them.
      addPcFlowsToConstraint(node.getSender(), hostLabel);
    }
    return null;
  }

  @Override
  protected Void leave(LoopNode node, CheckStmtVisitor visitor, Void body) {
    return null;
  }

  @Override
  protected Void leave(BreakNode node, CheckStmtVisitor visitor, LabelVariable loopLabel) {
    setTrustLabel(node, pc); // TODO: do we want pc or loopLabel here?
    addPcFlowsToConstraint(node, loopLabel);
    return null;
  }

  @Override
  protected Void leave(BlockNode node, CheckStmtVisitor visitor, Iterable<Void> statements) {
    setTrustLabel(node, pc);
    return null;
  }

  @Override
  protected Void leave(AssertNode node, CheckStmtVisitor visitor, AtomicLabelTerm expression) {
    setTrustLabel(node, expression);
    // TODO: should we leak pc here since this terminates the program?
    return null;
  }

  @Override
  public Void visit(IfNode node) {
    final CheckStmtVisitor visitor = enter(node);
    final AtomicLabelTerm guard = node.getGuard().accept(visitor.getExpressionVisitor());

    final LabelVariable newPc = constraintSystem.addNewVariable(nameGenerator.getFreshName("pc"));
    setTrustLabel(node, newPc);
    addPcFlowsToConstraint(node, newPc);
    addOutputFlowsToConstraint(node.getGuard(), guard, newPc);

    // Check branches under modified pc
    final CheckStmtVisitor branchVisitor = new CheckStmtVisitor(visitor, newPc);
    node.getThenBranch().accept(branchVisitor);
    node.getElseBranch().accept(branchVisitor);

    return null;
  }

  /**
   * A wrapper around AST nodes whose only job is to pretty print the node when {@link
   * Object#toString()} is called. Instances of this wrapper are used as variable labels in the
   * constraint solver so we get more readable debug output.
   */
  private static final class PrettyNodeWrapper {
    private final ImpAstNode node;

    PrettyNodeWrapper(ImpAstNode node) {
      this.node = node;
    }

    @Override
    public String toString() {
      return Printer.run(node);
    }
  }

  private final class CheckReferenceVisitor
      extends AbstractReferenceVisitor<CheckReferenceVisitor, AtomicLabelTerm, AtomicLabelTerm> {

    @Override
    protected ExprVisitor<AtomicLabelTerm> getExpressionVisitor() {
      return CheckStmtVisitor.this.getExpressionVisitor();
    }

    @Override
    protected CheckReferenceVisitor enter(ReferenceNode node) {
      return this;
    }

    @Override
    protected AtomicLabelTerm leave(Variable node, CheckReferenceVisitor visitor) {
      return get(node);
    }

    @Override
    protected AtomicLabelTerm leave(
        ArrayIndexingNode node,
        CheckReferenceVisitor visitor,
        AtomicLabelTerm array,
        AtomicLabelTerm index) {

      // The array gets to learn the the index.
      // TODO: this checks integrity also. Do we need that?
      addOutputFlowsToConstraint(node.getIndex(), index, array);

      return array;
    }
  }

  private final class CheckExprVisitor
      extends AbstractExprVisitor<CheckExprVisitor, AtomicLabelTerm, AtomicLabelTerm> {

    @Override
    protected ReferenceVisitor<AtomicLabelTerm> getReferenceVisitor() {
      return CheckStmtVisitor.this.getReferenceVisitor();
    }

    @Override
    protected CheckExprVisitor enter(ExpressionNode node) {
      return this;
    }

    @Override
    protected AtomicLabelTerm leave(LiteralNode node, CheckExprVisitor visitor) {
      return setTrustLabelToFreshVariable(node);
    }

    @Override
    protected AtomicLabelTerm leave(
        ReadNode node, CheckExprVisitor visitor, AtomicLabelTerm reference) {
      final LabelVariable l = setTrustLabelToFreshVariable(node);

      addOutputFlowsToConstraint(node.getReference(), reference, l);

      // We leak the pc every time we read a variable.
      // TODO: this checks integrity also. Do we need that?
      addPcFlowsToConstraint(node.getReference(), reference);

      return l;
    }

    @Override
    protected AtomicLabelTerm leave(
        NotNode node, CheckExprVisitor visitor, AtomicLabelTerm expression) {
      setTrustLabel(node, expression);
      return expression;
    }

    @Override
    protected AtomicLabelTerm leave(
        BinaryExpressionNode node,
        CheckExprVisitor visitor,
        AtomicLabelTerm lhs,
        AtomicLabelTerm rhs) {
      final LabelVariable l = setTrustLabelToFreshVariable(node);

      addOutputFlowsToConstraint(node.getLhs(), lhs, l);
      addOutputFlowsToConstraint(node.getRhs(), rhs, l);

      return l;
    }

    @Override
    protected AtomicLabelTerm leave(
        DowngradeNode node, CheckExprVisitor visitor, AtomicLabelTerm expression) {
      final AtomicLabelTerm from =
          node.getFromLabel() != null ? LabelConstant.create(node.getFromLabel()) : expression;
      final LabelConstant to = LabelConstant.create(node.getToLabel());

      // TODO: Should this be from or to?
      setTrustLabel(node, from);

      // pc is always leaked to the output label
      addPcFlowsToConstraint(node, to);

      // From label must match the expression label if it is specified
      if (node.getFromLabel() != null) {
        constraintSystem.addEqualToConstraint(
            expression,
            LabelConstant.create(node.getFromLabel()),
            (actual, expected) -> new LabelMismatchError(node.getExpression(), actual, expected));
      }

      // Non-malleable downgrade constraints
      constraintSystem.addFlowsToConstraint(
          from,
          from.swap().join(to.getValue()),
          (ignore1, ignore2) -> new MalleableDowngradeError(node));
      constraintSystem.addFlowsToConstraint(
          from,
          pc.swap().join(to.getValue()),
          (ignore1, ignore2) -> new MalleableDowngradeError(node));

      // Check that single dimensional downgrades don't change the other dimension
      switch (node.getDowngradeType()) {
        case DECLASSIFY:
          constraintSystem.addEqualToConstraint(
              from.integrity(),
              to.integrity(),
              (in, out) -> new IntegrityChangingDeclassificationError(node, in, out));
          break;

        case ENDORSE:
          constraintSystem.addEqualToConstraint(
              from.confidentiality(),
              to.confidentiality(),
              (in, out) -> new ConfidentialityChangingEndorsementError(node, in, out));
          break;

        case BOTH:
        default:
          break;
      }

      return to;
    }
  }
}
