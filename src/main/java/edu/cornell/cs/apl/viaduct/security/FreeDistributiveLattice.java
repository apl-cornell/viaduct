package edu.cornell.cs.apl.viaduct.security;

import edu.cornell.cs.apl.viaduct.util.CoHeytingAlgebra;
import io.vavr.collection.HashSet;
import io.vavr.collection.Set;
import java.util.Objects;

/**
 * The free distributive lattice over an arbitrary set {@code A} of elements. In addition to lattice
 * identities, the following hold:
 *
 * <p>{@code a /\ (b \/ c) == (a /\ b) \/ (a /\ c)}
 *
 * <p>{@code a \/ (b /\ c) == (a \/ b) /\ (a \/ c)}
 */
public final class FreeDistributiveLattice<A>
    implements CoHeytingAlgebra<FreeDistributiveLattice<A>> {
  private static final FreeDistributiveLattice<?> BOTTOM =
      new FreeDistributiveLattice<>(HashSet.of());

  private static final FreeDistributiveLattice<?> TOP =
      new FreeDistributiveLattice<>(HashSet.of(HashSet.of()));

  private final Set<Set<A>> joinOfMeets;

  FreeDistributiveLattice(A element) {
    this.joinOfMeets = HashSet.of(HashSet.of(element));
  }

  private FreeDistributiveLattice(Set<Set<A>> joinOfMeets) {
    this.joinOfMeets = removeRedundant(joinOfMeets);
  }

  @SuppressWarnings("unchecked")
  public static <T> FreeDistributiveLattice<T> top() {
    return (FreeDistributiveLattice<T>) TOP;
  }

  @SuppressWarnings("unchecked")
  public static <T> FreeDistributiveLattice<T> bottom() {
    return (FreeDistributiveLattice<T>) BOTTOM;
  }

  /** Remove redundant meets according to {@link static isRedundant}. */
  private static <A> Set<Set<A>> removeRedundant(Set<Set<A>> joinOfMeets) {
    return joinOfMeets.filter((meet) -> !isRedundant(joinOfMeets, meet));
  }

  /**
   * Given {@code m_1 \/ m_2 \/ ... \/ m_n}, if any {@code m_i} is a strict subset of {@code m_j},
   * then {@code m_j} is redundant.
   */
  private static <A> boolean isRedundant(Set<Set<A>> joinOfMeets, Set<A> j) {
    for (Set<A> i : joinOfMeets) {
      if (!i.equals(j) && j.containsAll(i)) {
        return true;
      }
    }
    return false;
  }

  private static <A> String meetToString(Set<A> meet, String meetOp) {
    String meetStr = String.format(" %s ", meetOp);
    final String body = String.join(meetStr, meet.toArray().map(Objects::toString));
    return meet.length() > 1 ? "(" + body + ")" : body;
  }

  @Override
  public boolean lessThanOrEqualTo(FreeDistributiveLattice<A> that) {
    return this.join(that).equals(that);
  }

  @Override
  public FreeDistributiveLattice<A> join(FreeDistributiveLattice<A> with) {
    return new FreeDistributiveLattice<>(this.joinOfMeets.union(with.joinOfMeets));
  }

  @Override
  public FreeDistributiveLattice<A> meet(FreeDistributiveLattice<A> with) {
    Set<Set<A>> candidates = HashSet.empty();

    for (Set<A> meet1 : this.joinOfMeets) {
      for (Set<A> meet2 : with.joinOfMeets) {
        candidates = candidates.add(meet1.union(meet2));
      }
    }

    return new FreeDistributiveLattice<>(candidates);
  }

  @Override
  public FreeDistributiveLattice<A> subtract(FreeDistributiveLattice<A> that) {
    // TODO: implement
    return null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof FreeDistributiveLattice)) {
      return false;
    }

    final FreeDistributiveLattice that = (FreeDistributiveLattice) o;
    return Objects.equals(this.joinOfMeets, that.joinOfMeets);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.joinOfMeets);
  }

  @Override
  public String toString() {
    return toString("|", "&");
  }

  /**
   * Return string representation using the given symbols for join and meet.
   *
   * @param joinOp symbol to use for {@link #join}
   * @param meetOp symbol to use for {@link #meet}
   */
  public String toString(String joinOp, String meetOp) {
    if (this.equals(top())) {
      return "⊤";
    } else if (this.equals(bottom())) {
      return "⊥";
    } else {
      String joinStr = String.format(" %s ", joinOp);
      final String body =
          String.join(joinStr, this.joinOfMeets.toArray().map(meet -> meetToString(meet, meetOp)));
      return this.joinOfMeets.length() > 1 ? "(" + body + ")" : body;
    }
  }
}
