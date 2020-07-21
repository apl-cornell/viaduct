package edu.cornell.cs.apl.viaduct.security.solver;

import com.google.auto.value.AutoValue;
import edu.cornell.cs.apl.viaduct.algebra.FreeDistributiveLattice;
import edu.cornell.cs.apl.viaduct.algebra.solver.LeftHandTerm;
import edu.cornell.cs.apl.viaduct.algebra.solver.RightHandTerm;
import edu.cornell.cs.apl.viaduct.security.Label;
import edu.cornell.cs.apl.viaduct.security.Principal;
import java.util.Map;

@AutoValue
abstract class ConstantJoinVariableTerm extends LabelTerm {
  static ConstantJoinVariableTerm create(Label lhs, LabelVariable rhs) {
    return new AutoValue_ConstantJoinVariableTerm(lhs, rhs);
  }

  abstract Label getLhs();

  abstract LabelVariable getRhs();

  @Override
  public final Label getValue(Map<LabelVariable, Label> solution) {
    return getLhs().join(getRhs().getValue(solution));
  }

  @Override
  public LabelTerm confidentiality() {
    return create(getLhs().confidentiality(), getRhs().confidentiality());
  }

  @Override
  public LabelTerm integrity() {
    return create(getLhs().integrity(), getRhs().integrity());
  }

  @Override
  final LeftHandTerm<FreeDistributiveLattice<Principal>> getConfidentialityComponent() {
    return getRhs().getConfidentialityComponent().meet(getLhs().getConfidentialityComponent());
  }

  @Override
  final RightHandTerm<FreeDistributiveLattice<Principal>> getIntegrityComponent() {
    return getRhs().getIntegrityComponent().join(getLhs().getIntegrityComponent());
  }
}
