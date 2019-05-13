package edu.cornell.cs.apl.viaduct.security;

import java.util.Objects;

/**
 * A lattice for information flow security. It is a standard bounded lattice that additionally
 * supports confidentiality and integrity projections. Information flows from less restrictive
 * contexts to more restrictive ones.
 *
 * <p>In an information flow lattice, {@link static bottom()} is the least restrictive context and
 * corresponds to public and trusted information. Dually, {@link static top()} is private and
 * untrusted. Everything else lies in between these two extremes.
 */
public class Label<P> implements Lattice<Label<P>> {
  private static final Label bottom =
      new Label<>(FreeDistributiveLattice.top(), FreeDistributiveLattice.bottom());

  private static final Label top =
      new Label<>(FreeDistributiveLattice.bottom(), FreeDistributiveLattice.top());

  private final FreeDistributiveLattice<P> confidentiality;
  private final FreeDistributiveLattice<P> integrity;

  /** Label corresponding to a single given principal. */
  public Label(P principal) {
    final FreeDistributiveLattice<P> component = new FreeDistributiveLattice<>(principal);
    this.confidentiality = component;
    this.integrity = component;
  }

  private Label(FreeDistributiveLattice<P> confidentiality, FreeDistributiveLattice<P> integrity) {
    this.confidentiality = confidentiality;
    this.integrity = integrity;
  }

  /**
   * Label corresponding to the joint authority of all given principals.
   *
   * <p>Equivalent to combining all principals with {@link #and(Label)}.
   */
  @SafeVarargs
  public static <P> Label<P> and(P... principals) {
    Label<P> result = new Label<>(FreeDistributiveLattice.top(), FreeDistributiveLattice.top());
    for (P principal : principals) {
      result = result.and(new Label<>(principal));
    }
    return result;
  }

  /**
   * Label corresponding to the disjunctive authority of all given principals.
   *
   * <p>Equivalent to combining all principals with {@link #or(Label)}.
   */
  @SafeVarargs
  public static <P> Label or(P... principals) {
    Label<P> result =
        new Label<>(FreeDistributiveLattice.bottom(), FreeDistributiveLattice.bottom());
    for (P principal : principals) {
      result = result.or(new Label<>(principal));
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  public static <P> Label<P> bottom() {
    return bottom;
  }

  @SuppressWarnings("unchecked")
  public static <P> Label<P> top() {
    return top;
  }

  /** Check if information flow from {@code this} to {@code other} is safe. */
  public boolean flowsTo(Label<P> other) {
    return this.lessThanOrEqualTo(other);
  }

  @Override
  public boolean lessThanOrEqualTo(Label<P> other) {
    return other.confidentiality.lessThanOrEqualTo(this.confidentiality)
        && this.integrity.lessThanOrEqualTo(other.integrity);
  }

  @Override
  public Label<P> join(Label<P> with) {
    return new Label<>(
        this.confidentiality.meet(with.confidentiality), this.integrity.join(with.integrity));
  }

  @Override
  public Label<P> meet(Label<P> with) {
    return new Label<>(
        this.confidentiality.join(with.confidentiality), this.integrity.meet(with.integrity));
  }

  /**
   * The confidentiality component.
   *
   * <p>Keeps confidentiality the same while setting integrity to minimum.
   */
  public Label<P> confidentiality() {
    return new Label<>(this.confidentiality, FreeDistributiveLattice.top());
  }

  /**
   * The integrity component.
   *
   * <p>Keeps integrity the same while setting confidentiality to minimum.
   */
  public Label<P> integrity() {
    return new Label<>(FreeDistributiveLattice.top(), this.integrity);
  }

  /**
   * Decides if {@code this} (interpreted as a principal) is trusted to enforce {@code other}'s
   * policies.
   */
  public boolean actsFor(Label<P> other) {
    return this.confidentiality.lessThanOrEqualTo(other.confidentiality)
        && this.integrity.lessThanOrEqualTo(other.integrity);
  }

  /**
   * The least powerful principal that can act for both {@code this} and {@code with}. That is,
   * {@code and} denotes a conjunction of authority.
   */
  public Label<P> and(Label<P> with) {
    final FreeDistributiveLattice<P> confidentiality =
        this.confidentiality.meet(with.confidentiality);
    final FreeDistributiveLattice<P> integrity = this.integrity.meet(with.integrity);
    return new Label<>(confidentiality, integrity);
  }

  /**
   * The most powerful principal both {@code this} and {@code with} can act for. That is, {@code or}
   * denotes a disjunction of authority.
   */
  public Label<P> or(Label<P> with) {
    final FreeDistributiveLattice<P> confidentiality =
        this.confidentiality.join(with.confidentiality);
    final FreeDistributiveLattice<P> integrity = this.integrity.join(with.integrity);
    return new Label<>(confidentiality, integrity);
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
    final String confidentialityString = this.confidentiality.toString() + "<-";
    final String integrityString = this.integrity.toString() + "->";

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
