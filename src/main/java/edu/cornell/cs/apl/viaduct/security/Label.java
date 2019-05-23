package edu.cornell.cs.apl.viaduct.security;

import java.util.Objects;

/**
 * A lattice for information flow security. It is a standard bounded lattice that additionally
 * supports confidentiality and integrity projections. Information flows from less restrictive
 * contexts to more restrictive ones.
 */
public class Label implements Lattice<Label>, TrustLattice<Label> {
  private static final Label BOTTOM =
      new Label(FreeDistributiveLattice.top(), FreeDistributiveLattice.bottom());

  private static final Label TOP =
      new Label(FreeDistributiveLattice.bottom(), FreeDistributiveLattice.top());

  private final FreeDistributiveLattice<Principal> confidentiality;
  private final FreeDistributiveLattice<Principal> integrity;

  /** Label corresponding to a single given principal. */
  public Label(Principal principal) {
    final FreeDistributiveLattice<Principal> component = new FreeDistributiveLattice<>(principal);
    this.confidentiality = component;
    this.integrity = component;
  }

  private Label(
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

  /** Check if information flow from {@code this} to {@code other} is safe. */
  public boolean flowsTo(Label other) {
    return this.lessThanOrEqualTo(other);
  }

  @Override
  public boolean lessThanOrEqualTo(Label other) {
    return other.confidentiality.lessThanOrEqualTo(this.confidentiality)
        && this.integrity.lessThanOrEqualTo(other.integrity);
  }

  @Override
  public Label join(Label with) {
    return new Label(
        this.confidentiality.meet(with.confidentiality), this.integrity.join(with.integrity));
  }

  @Override
  public Label meet(Label with) {
    return new Label(
        this.confidentiality.join(with.confidentiality), this.integrity.meet(with.integrity));
  }

  /**
   * The confidentiality component.
   *
   * <p>Keeps confidentiality the same while setting integrity to minimum.
   */
  public Label confidentiality() {
    return new Label(this.confidentiality, FreeDistributiveLattice.top());
  }

  /**
   * The integrity component.
   *
   * <p>Keeps integrity the same while setting confidentiality to minimum.
   */
  public Label integrity() {
    return new Label(FreeDistributiveLattice.top(), this.integrity);
  }

  @Override
  public boolean actsFor(Label other) {
    return this.confidentiality.lessThanOrEqualTo(other.confidentiality)
        && this.integrity.lessThanOrEqualTo(other.integrity);
  }

  @Override
  public Label and(Label with) {
    final FreeDistributiveLattice<Principal> confidentiality =
        this.confidentiality.meet(with.confidentiality);
    final FreeDistributiveLattice<Principal> integrity = this.integrity.meet(with.integrity);
    return new Label(confidentiality, integrity);
  }

  @Override
  public Label or(Label with) {
    final FreeDistributiveLattice<Principal> confidentiality =
        this.confidentiality.join(with.confidentiality);
    final FreeDistributiveLattice<Principal> integrity = this.integrity.join(with.integrity);
    return new Label(confidentiality, integrity);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final Label that = (Label) o;
    return this.confidentiality.equals(that.confidentiality)
        && this.integrity.equals(that.integrity);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.confidentiality, this.integrity);
  }

  @Override
  public String toString() {
    final String confidentialityString = this.confidentiality.toString() + "->";
    final String integrityString = this.integrity.toString() + "<-";

    if (this.confidentiality.equals(this.integrity)) {
      return this.confidentiality.toString();
    } else if (this.equals(this.confidentiality())) {
      return confidentialityString;
    } else if (this.equals(this.integrity())) {
      return integrityString;
    } else {
      return String.format("(%s, %s)", confidentialityString, integrityString);
    }
  }
}
