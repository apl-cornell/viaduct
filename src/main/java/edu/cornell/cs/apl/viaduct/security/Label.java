package edu.cornell.cs.apl.viaduct.security;

import edu.cornell.cs.apl.viaduct.util.Lattice;
import java.util.Objects;

/**
 * A lattice for information flow security. It is a standard bounded lattice that additionally
 * supports confidentiality and integrity projections. Information flows from less restrictive
 * contexts to more restrictive ones.
 *
 * <p>{@link #top()}, {@link #bottom()}, {@link #meet(Label)}, and {@link #join(Label)} talk about
 * information flow.
 *
 * <p>{@link #strongest()}, {@link #weakest()}, {@link #and(Label)}, and {@link #or(Label)} talk
 * about trust.
 */
public class Label implements Lattice<Label>, TrustLattice<Label> {
  private static final Label WEAKEST =
      new Label(FreeDistributiveLattice.bottom(), FreeDistributiveLattice.bottom());

  private static final Label STRONGEST =
      new Label(FreeDistributiveLattice.top(), FreeDistributiveLattice.top());

  private static final Label BOTTOM = new Label(weakest().confidentiality, strongest().integrity);

  private static final Label TOP = new Label(strongest().confidentiality, weakest().integrity);

  private final FreeDistributiveLattice<Principal> confidentiality;
  private final FreeDistributiveLattice<Principal> integrity;

  /** Label corresponding to a single given principal. */
  public Label(Principal principal) {
    final FreeDistributiveLattice<Principal> component = new FreeDistributiveLattice<>(principal);
    this.confidentiality = component;
    this.integrity = component;
  }

  public Label(
      FreeDistributiveLattice<Principal> confidentiality,
      FreeDistributiveLattice<Principal> integrity) {
    this.confidentiality = confidentiality;
    this.integrity = integrity;
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
   * <p>This is represented as ⊥, and is the unit for {@link #and(Label)}.
   */
  public static Label weakest() {
    return WEAKEST;
  }

  /**
   * The most powerful principal, i.e. secret and trusted.
   *
   * <p>This is represented as ⊤, and is the unit for {@link #or(Label)}.
   */
  public static Label strongest() {
    return STRONGEST;
  }

  /**
   * Return the confidentiality component in the underlying lattice.
   *
   * <p>Unlike {@link #confidentiality()}, the result is not a {@link Label}.
   */
  public FreeDistributiveLattice<Principal> getConfidentiality() {
    return confidentiality;
  }

  /**
   * Return the integrity component in the underlying lattice.
   *
   * <p>Unlike {@link #integrity()}, the result is not a {@link Label}.
   */
  public FreeDistributiveLattice<Principal> getIntegrity() {
    return integrity;
  }

  /** Check if information flow from {@code this} to {@code other} is safe. */
  public boolean flowsTo(Label other) {
    return this.confidentiality.lessThanOrEqualTo(other.confidentiality)
        && other.integrity.lessThanOrEqualTo(this.integrity);
  }

  @Override
  public boolean lessThanOrEqualTo(Label other) {
    return this.flowsTo(other);
  }

  @Override
  public Label join(Label with) {
    return new Label(
        this.confidentiality.join(with.confidentiality), this.integrity.meet(with.integrity));
  }

  @Override
  public Label meet(Label with) {
    return new Label(
        this.confidentiality.meet(with.confidentiality), this.integrity.join(with.integrity));
  }

  /**
   * The confidentiality component.
   *
   * <p>Keeps confidentiality the same while setting integrity to weakest integrity.
   */
  public Label confidentiality() {
    return new Label(this.confidentiality, weakest().integrity);
  }

  /**
   * The integrity component.
   *
   * <p>Keeps integrity the same while setting confidentiality to weakest confidentiality.
   */
  public Label integrity() {
    return new Label(weakest().confidentiality, this.integrity);
  }

  @Override
  public boolean actsFor(Label other) {
    return other.confidentiality.lessThanOrEqualTo(this.confidentiality)
        && other.integrity.lessThanOrEqualTo(this.integrity);
  }

  @Override
  public Label and(Label with) {
    final FreeDistributiveLattice<Principal> confidentiality =
        this.confidentiality.join(with.confidentiality);
    final FreeDistributiveLattice<Principal> integrity = this.integrity.join(with.integrity);
    return new Label(confidentiality, integrity);
  }

  @Override
  public Label or(Label with) {
    final FreeDistributiveLattice<Principal> confidentiality =
        this.confidentiality.meet(with.confidentiality);
    final FreeDistributiveLattice<Principal> integrity = this.integrity.meet(with.integrity);
    return new Label(confidentiality, integrity);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof Label)) {
      return false;
    }

    final Label that = (Label) o;
    return Objects.equals(this.confidentiality, that.confidentiality)
        && Objects.equals(this.integrity, that.integrity);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.confidentiality, this.integrity);
  }

  @Override
  public String toString() {
    final String confidentialityStr = this.confidentiality.toString("&", "|");
    final String integrityStr = this.integrity.toString("&", "|");

    String expression;
    if (this.equals(weakest())) {
      expression = "";

    } else if (this.confidentiality.equals(this.integrity)) {
      expression = confidentialityStr;

    } else if (this.equals(this.confidentiality())) {
      expression = String.format("%s->", confidentialityStr);

    } else if (this.equals(this.integrity())) {
      expression = String.format("%s<-", integrityStr);

    } else {
      expression = String.format("%s-> & %s<-", confidentialityStr, integrityStr);
    }

    return String.format("{%s}", expression);
  }
}
