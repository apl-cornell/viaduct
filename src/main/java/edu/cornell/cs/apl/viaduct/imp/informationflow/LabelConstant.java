package edu.cornell.cs.apl.viaduct.imp.informationflow;

import edu.cornell.cs.apl.viaduct.security.FreeDistributiveLattice;
import edu.cornell.cs.apl.viaduct.security.Label;
import edu.cornell.cs.apl.viaduct.security.Principal;
import edu.cornell.cs.apl.viaduct.security.solver.ConstantTerm;

/** A statically known constant. */
final class LabelConstant extends LabelTerm {
  private final Label value;

  LabelConstant(Label value) {
    this.value = value;
  }

  @Override
  public Label getValue() {
    return value;
  }

  @Override
  ConstantTerm<FreeDistributiveLattice<Principal>> getConfidentiality() {
    return ConstantTerm.create(value.getConfidentiality());
  }

  @Override
  ConstantTerm<FreeDistributiveLattice<Principal>> getIntegrity() {
    return ConstantTerm.create(value.getIntegrity());
  }
}
