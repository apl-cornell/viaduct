package edu.cornell.cs.apl.viaduct.security.solver;

import edu.cornell.cs.apl.viaduct.algebra.FreeDistributiveLattice;
import edu.cornell.cs.apl.viaduct.algebra.solver.LeftHandTerm;
import edu.cornell.cs.apl.viaduct.algebra.solver.RightHandTerm;
import edu.cornell.cs.apl.viaduct.security.Label;
import edu.cornell.cs.apl.viaduct.security.Principal;
import java.util.Map;

/** A symbolic representation of a label expression. */
public abstract class LabelTerm {
  /** Returns the value of this term given an assignment of values to every variable in the term. */
  public abstract Label getValue(Map<LabelVariable, Label> solution);

  /** Term that corresponds to performing {@link Label#confidentiality()}. */
  public abstract LabelTerm confidentiality();

  /** Term that corresponds to performing {@link Label#integrity()}. */
  public abstract LabelTerm integrity();

  /** Returns a term representing the confidentiality component. */
  abstract LeftHandTerm<FreeDistributiveLattice<Principal>> getConfidentialityComponent();

  /** Returns a term representing the integrity component. */
  abstract RightHandTerm<FreeDistributiveLattice<Principal>> getIntegrityComponent();
}
