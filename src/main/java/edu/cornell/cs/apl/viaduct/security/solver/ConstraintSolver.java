package edu.cornell.cs.apl.viaduct.security.solver;

import edu.cornell.cs.apl.viaduct.algebra.FreeDistributiveLattice;
import edu.cornell.cs.apl.viaduct.algebra.solver.ConstraintSystem;
import edu.cornell.cs.apl.viaduct.algebra.solver.VariableTerm;
import edu.cornell.cs.apl.viaduct.security.Label;
import edu.cornell.cs.apl.viaduct.security.Principal;
import io.vavr.Function2;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Given a set of information flow constraints, finds a label assignment to all variables that
 * minimizes the trust assigned to each variable (if one exists).
 *
 * @param <T> type of exceptions thrown when there are unsatisfiable constraints
 */
public final class ConstraintSolver<T extends Throwable> {

  private final ConstraintSystem<FreeDistributiveLattice<Principal>, T> constraintSystem =
      new ConstraintSystem<>(FreeDistributiveLattice.top());

  /** Set of variables that appear in the constraints. */
  private final Set<LabelVariable> variables = new HashSet<>();

  /**
   * Find the least trust solution to the set of constraints in the system.
   *
   * @return mapping from variables to minimal trust labels that satisfy all constraints
   * @throws T if there are unsatisfiable constraints
   */
  public Map<LabelVariable, Label> solve() throws T {
    final Map<VariableTerm<FreeDistributiveLattice<Principal>>, FreeDistributiveLattice<Principal>>
        componentSolutions = constraintSystem.solve();

    final Map<LabelVariable, Label> solutions = new HashMap<>();
    for (LabelVariable variable : variables) {
      final FreeDistributiveLattice<Principal> confidentiality =
          variable.getConfidentialityComponent().getValue(componentSolutions);
      final FreeDistributiveLattice<Principal> integrity =
          variable.getIntegrityComponent().getValue(componentSolutions);
      solutions.put(variable, Label.create(confidentiality, integrity));
    }
    return solutions;
  }

  /**
   * Create a fresh variable and add it to the system.
   *
   * @param label an arbitrary object to use as a label during debugging
   * @return the freshly created variable
   */
  public LabelVariable addNewVariable(Object label) {
    final LabelVariable variable =
        LabelVariable.create(
            constraintSystem.addNewVariable(new ConfidentialityWrapper(label)),
            constraintSystem.addNewVariable(new IntegrityWrapper(label)));
    this.variables.add(variable);
    return variable;
  }

  /**
   * Add the constraint {@code lhs.flowsTo(rhs)} to the system.
   *
   * @param failWith a function that generates the exception to throw if this constraint is
   *     unsatisfiable. The function will be given the best estimates for the values of {@code lhs}
   *     and {@code rhs}.
   */
  public void addFlowsToConstraint(
      AtomicLabelTerm lhs, LabelTerm rhs, Function2<Label, Label, T> failWith) {
    addConfidentialityFlowsToConstraint(lhs, rhs, failWith);
    addIntegrityFlowsToConstraint(lhs, rhs, failWith);
  }

  /**
   * Add the constraint {@code lhs == rhs} to the system.
   *
   * @param failWith same as in {@link #addFlowsToConstraint(AtomicLabelTerm, LabelTerm, Function2)}
   */
  public void addEqualToConstraint(
      AtomicLabelTerm lhs, AtomicLabelTerm rhs, Function2<Label, Label, T> failWith) {
    addFlowsToConstraint(lhs, rhs, failWith);
    addFlowsToConstraint(rhs, lhs, (to, from) -> failWith.apply(from, to));
  }

  /**
   * Add the constraint {@code lhs.confidentiality().flowsTo(rhs.confidentiality())}.
   *
   * @param failWith same as in {@link #addFlowsToConstraint(AtomicLabelTerm, LabelTerm, Function2)}
   */
  private void addConfidentialityFlowsToConstraint(
      AtomicLabelTerm lhs, LabelTerm rhs, Function2<Label, Label, T> failWith) {
    constraintSystem.addLessThanOrEqualToConstraint(
        rhs.getConfidentialityComponent(),
        lhs.getConfidentialityComponent(),
        (to, from) ->
            failWith.apply(Label.fromConfidentiality(from), Label.fromConfidentiality(to)));
  }

  /**
   * Add the constraint {@code lhs.integrity().flowsTo(rhs.integrity())}.
   *
   * @param failWith same as in {@link #addFlowsToConstraint(AtomicLabelTerm, LabelTerm, Function2)}
   */
  private void addIntegrityFlowsToConstraint(
      AtomicLabelTerm lhs, LabelTerm rhs, Function2<Label, Label, T> failWith) {
    constraintSystem.addLessThanOrEqualToConstraint(
        lhs.getIntegrityComponent(),
        rhs.getIntegrityComponent(),
        (from, to) -> failWith.apply(Label.fromIntegrity(from), Label.fromIntegrity(to)));
  }

  /** Label wrapper for variables that track confidentiality. */
  private static final class ConfidentialityWrapper {
    private final Object label;

    ConfidentialityWrapper(Object label) {
      this.label = label;
    }

    @Override
    public String toString() {
      return "confidentiality " + label.toString();
    }
  }

  /** Label wrapper for variables that track integrity. */
  private static final class IntegrityWrapper {
    private final Object label;

    IntegrityWrapper(Object label) {
      this.label = label;
    }

    @Override
    public String toString() {
      return "integrity " + label.toString();
    }
  }
}
