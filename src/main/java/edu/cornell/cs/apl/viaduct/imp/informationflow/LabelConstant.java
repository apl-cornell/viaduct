package edu.cornell.cs.apl.viaduct.imp.informationflow;

import edu.cornell.cs.apl.viaduct.security.FreeDistributiveLattice;
import edu.cornell.cs.apl.viaduct.security.Label;
import edu.cornell.cs.apl.viaduct.security.Principal;
import edu.cornell.cs.apl.viaduct.security.solver.ConstantTerm;

/** A statically known constant. */
final class LabelConstant extends LabelTerm {
  private final ConstantTerm<FreeDistributiveLattice<Principal>> confidentiality;
  private final ConstantTerm<FreeDistributiveLattice<Principal>> integrity;
  private final Label value;

  LabelConstant(
      ConstantTerm<FreeDistributiveLattice<Principal>> confidentiality,
      ConstantTerm<FreeDistributiveLattice<Principal>> integrity)
  {
    this.confidentiality = confidentiality;
    this.integrity = integrity;
    this.value = new Label(confidentiality.getValue(), integrity.getValue());
  }

  @Override
  public Label getValue() {
    return value;
  }

  @Override
  ConstantTerm<FreeDistributiveLattice<Principal>> getConfidentiality() {
    return this.confidentiality;
  }

  @Override
  ConstantTerm<FreeDistributiveLattice<Principal>> getIntegrity() {
    return this.integrity;
  }
}
