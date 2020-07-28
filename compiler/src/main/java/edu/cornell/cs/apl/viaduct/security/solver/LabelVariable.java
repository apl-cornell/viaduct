package edu.cornell.cs.apl.viaduct.security.solver;

import com.google.auto.value.AutoValue;
import edu.cornell.cs.apl.viaduct.algebra.FreeDistributiveLattice;
import edu.cornell.cs.apl.viaduct.algebra.solver.AtomicTerm;
import edu.cornell.cs.apl.viaduct.algebra.solver.ConstantTerm;
import edu.cornell.cs.apl.viaduct.security.Label;
import edu.cornell.cs.apl.viaduct.security.Principal;
import java.util.Map;

/**
 * A stand-in for an unknown label. The solver will assign an actual value to each instance.
 *
 * @see ConstraintSolver#addNewVariable(Object) for creating instances.
 */
@AutoValue
public abstract class LabelVariable extends AtomicLabelTerm {
  static LabelVariable create(
      AtomicTerm<FreeDistributiveLattice<Principal>> confidentiality,
      AtomicTerm<FreeDistributiveLattice<Principal>> integrity) {
    return new AutoValue_LabelVariable(confidentiality, integrity);
  }

  @Override
  abstract AtomicTerm<FreeDistributiveLattice<Principal>> getConfidentialityComponent();

  @Override
  abstract AtomicTerm<FreeDistributiveLattice<Principal>> getIntegrityComponent();

  @Override
  public final Label getValue(Map<LabelVariable, Label> solution) {
    return solution.get(this);
  }

  @Override
  public LabelVariable confidentiality() {
    return create(
        getConfidentialityComponent(),
        ConstantTerm.create(Label.getWeakest().getIntegrityComponent()));
  }

  @Override
  public LabelVariable integrity() {
    return create(
        ConstantTerm.create(Label.getWeakest().getConfidentialityComponent()),
        getIntegrityComponent());
  }

  @Override
  public final AtomicLabelTerm swap() {
    return create(getIntegrityComponent(), getConfidentialityComponent());
  }

  @Override
  public final ConstantJoinVariableTerm join(Label that) {
    return ConstantJoinVariableTerm.create(that, this);
  }
}
