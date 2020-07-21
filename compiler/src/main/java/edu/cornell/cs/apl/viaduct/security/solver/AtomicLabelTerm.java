package edu.cornell.cs.apl.viaduct.security.solver;

import edu.cornell.cs.apl.viaduct.algebra.FreeDistributiveLattice;
import edu.cornell.cs.apl.viaduct.algebra.solver.AtomicTerm;
import edu.cornell.cs.apl.viaduct.security.Label;
import edu.cornell.cs.apl.viaduct.security.Principal;

/** Atomic terms like constants and variables but joins. */
public abstract class AtomicLabelTerm extends LabelTerm {
  @Override
  public abstract AtomicLabelTerm confidentiality();

  @Override
  public abstract AtomicLabelTerm integrity();

  /** Term that corresponds to performing {@link Label#swap()}. */
  public abstract AtomicLabelTerm swap();

  /** Term that corresponds to the {@link Label#join(Label)} of {@code this} and {@code that}. */
  public abstract LabelTerm join(Label that);

  @Override
  abstract AtomicTerm<FreeDistributiveLattice<Principal>> getConfidentialityComponent();

  @Override
  abstract AtomicTerm<FreeDistributiveLattice<Principal>> getIntegrityComponent();
}
