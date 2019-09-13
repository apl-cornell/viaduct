package edu.cornell.cs.apl.viaduct.security;

import com.google.auto.value.AutoValue;
import edu.cornell.cs.apl.viaduct.algebra.FreeDistributiveLattice;
import edu.cornell.cs.apl.viaduct.algebra.Lattice;

/**
 * A lattice for information flow security. This is a standard bounded lattice that additionally
 * supports confidentiality and integrity projections. Information flows from less restrictive
 * contexts to more restrictive ones.
 *
 * <p>{@link #top()}, {@link #bottom()}, {@link #meet(Label)}, and {@link #join(Label)} talk about
 * information flow.
 *
 * <p>{@link #weakest()}, {@link #strongest()}, {@link #and(Label)}, and {@link #or(Label)} talk
 * about trust.
 */
@AutoValue
public abstract class Label implements Lattice<Label>, TrustLattice<Label> {
  // public, untrusted
  private static final Label WEAKEST =
      create(FreeDistributiveLattice.top(), FreeDistributiveLattice.top());

  // secret, trusted
  private static final Label STRONGEST =
      create(FreeDistributiveLattice.bottom(), FreeDistributiveLattice.bottom());

  // public, trusted
  private static final Label BOTTOM =
      create(weakest().getConfidentialityComponent(), strongest().getIntegrityComponent());

  // secret, untrusted
  private static final Label TOP =
      create(strongest().getConfidentialityComponent(), weakest().getIntegrityComponent());

  /** Constructs a label corresponding to a single given principal. */
  public static Label create(Principal principal) {
    final FreeDistributiveLattice<Principal> component = FreeDistributiveLattice.create(principal);
    return create(component, component);
  }

  /** Constructs a label given the confidentiality and integrity components. */
  public static Label create(
      FreeDistributiveLattice<Principal> confidentiality,
      FreeDistributiveLattice<Principal> integrity) {
    return new AutoValue_Label(confidentiality, integrity);
  }

  /** Constructs a label given only the confidentiality component. Integrity is set to minimum. */
  public static Label fromConfidentiality(FreeDistributiveLattice<Principal> confidentiality) {
    return Label.create(confidentiality, Label.weakest().getIntegrityComponent());
  }

  /** Construct a label given only the integrity component. Confidentiality is set to minimum. */
  public static Label fromIntegrity(FreeDistributiveLattice<Principal> integrity) {
    return Label.create(Label.weakest().getConfidentialityComponent(), integrity);
  }

  /** The least restrictive data policy, i.e. public and trusted. */
  public static Label bottom() {
    return BOTTOM;
  }

  /** The most restrictive data policy, i.e. secret and untrusted. */
  public static Label top() {
    return TOP;
  }

  /**
   * The least powerful principal, i.e. public and untrusted.
   *
   * <p>This is represented as 1, and is the unit for {@link #and(Label)}.
   */
  public static Label weakest() {
    return WEAKEST;
  }

  /**
   * The most powerful principal, i.e. secret and trusted.
   *
   * <p>This is represented as 0, and is the unit for {@link #or(Label)}.
   */
  public static Label strongest() {
    return STRONGEST;
  }

  /**
   * Return the confidentiality component in the underlying lattice.
   *
   * <p>Unlike {@link #confidentiality()}, the result is not a {@link Label}.
   */
  public abstract FreeDistributiveLattice<Principal> getConfidentialityComponent();

  /**
   * Return the integrity component in the underlying lattice.
   *
   * <p>Unlike {@link #integrity()}, the result is not a {@link Label}.
   */
  public abstract FreeDistributiveLattice<Principal> getIntegrityComponent();

  /** Check if information flow from {@code this} to {@code that} is safe. */
  public final boolean flowsTo(Label that) {
    return that.confidentiality()
        .and(this.integrity())
        .actsFor(this.confidentiality().and(that.integrity()));
  }

  @Override
  public final boolean lessThanOrEqualTo(Label that) {
    return this.flowsTo(that);
  }

  @Override
  public final Label join(Label with) {
    return this.and(with).confidentiality().and(this.or(with).integrity());
  }

  @Override
  public final Label meet(Label with) {
    return this.or(with).confidentiality().and(this.and(with).integrity());
  }

  /**
   * The confidentiality component.
   *
   * <p>Keeps confidentiality the same while setting integrity to the weakest level.
   */
  public final Label confidentiality() {
    return create(this.getConfidentialityComponent(), weakest().getIntegrityComponent());
  }

  /**
   * The integrity component.
   *
   * <p>Keeps integrity the same while setting confidentiality to the weakest level.
   */
  public final Label integrity() {
    return create(weakest().getConfidentialityComponent(), this.getIntegrityComponent());
  }

  @Override
  public final boolean actsFor(Label other) {
    return this.getConfidentialityComponent().lessThanOrEqualTo(other.getConfidentialityComponent())
        && this.getIntegrityComponent().lessThanOrEqualTo(other.getIntegrityComponent());
  }

  @Override
  public final Label and(Label with) {
    final FreeDistributiveLattice<Principal> confidentiality =
        this.getConfidentialityComponent().meet(with.getConfidentialityComponent());
    final FreeDistributiveLattice<Principal> integrity =
        this.getIntegrityComponent().meet(with.getIntegrityComponent());
    return create(confidentiality, integrity);
  }

  @Override
  public final Label or(Label with) {
    final FreeDistributiveLattice<Principal> confidentiality =
        this.getConfidentialityComponent().join(with.getConfidentialityComponent());
    final FreeDistributiveLattice<Principal> integrity =
        this.getIntegrityComponent().join(with.getIntegrityComponent());
    return create(confidentiality, integrity);
  }

  /**
   * Switch the confidentiality and integrity components.
   *
   * <p>This is used to enforce robust declassification and transparent endorsement (a.k.a.
   * non-malleable information flow).
   */
  public final Label swap() {
    return Label.create(getIntegrityComponent(), getConfidentialityComponent());
  }

  @Override
  public final String toString() {
    final String confidentialityStr = this.getConfidentialityComponent().toString();
    final String integrityStr = this.getIntegrityComponent().toString();

    String expression;
    if (this.equals(weakest())) {
      expression = "";

    } else if (this.getConfidentialityComponent().equals(this.getIntegrityComponent())) {
      expression = confidentialityStr;

    } else if (this.equals(this.confidentiality())) {
      expression = confidentialityStr + "->";

    } else if (this.equals(this.integrity())) {
      expression = integrityStr + "<-";

    } else {
      expression = String.format("%s-> ∧ %s<-", confidentialityStr, integrityStr);
    }

    return String.format("{%s}", expression);
  }
}
