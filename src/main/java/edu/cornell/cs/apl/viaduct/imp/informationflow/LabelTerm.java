package edu.cornell.cs.apl.viaduct.imp.informationflow;

import edu.cornell.cs.apl.viaduct.security.FreeDistributiveLattice;
import edu.cornell.cs.apl.viaduct.security.Label;
import edu.cornell.cs.apl.viaduct.security.Principal;
import edu.cornell.cs.apl.viaduct.security.solver.ConstraintValue;

/** A constraint term that stands for an information flow label. */
public abstract class LabelTerm {
  /** Get the label value this term evaluates to. */
  public abstract Label getValue();

  abstract ConstraintValue<FreeDistributiveLattice<Principal>> getConfidentiality();

  abstract ConstraintValue<FreeDistributiveLattice<Principal>> getIntegrity();
}
