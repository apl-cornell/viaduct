package edu.cornell.cs.apl.viaduct.security.solver;

import com.google.auto.value.AutoValue;
import edu.cornell.cs.apl.viaduct.algebra.FreeDistributiveLattice;
import edu.cornell.cs.apl.viaduct.algebra.solver.ConstantTerm;
import edu.cornell.cs.apl.viaduct.security.Label;
import edu.cornell.cs.apl.viaduct.security.Principal;
import java.util.Map;

/** Terms representing literal label constants. */
@AutoValue
public abstract class LabelConstant extends AtomicLabelTerm {
  /** Create a term that represents the given value. */
  public static LabelConstant create(Label value) {
    return new AutoValue_LabelConstant(value);
  }

  public abstract Label getValue();

  @Override
  public final Label getValue(Map<LabelVariable, Label> solution) {
    return getValue();
  }

  @Override
  public LabelConstant confidentiality() {
    return create(getValue().confidentiality());
  }

  @Override
  public LabelConstant integrity() {
    return create(getValue().integrity());
  }

  @Override
  public final LabelConstant swap() {
    return create(getValue().swap());
  }

  @Override
  public final LabelConstant join(Label that) {
    return create(getValue().join(that));
  }

  @Override
  final ConstantTerm<FreeDistributiveLattice<Principal>> getConfidentialityComponent() {
    return new ConstantTerm<>(getValue().getConfidentialityComponent());
  }

  @Override
  final ConstantTerm<FreeDistributiveLattice<Principal>> getIntegrityComponent() {
    return new ConstantTerm<>(getValue().getIntegrityComponent());
  }
}
