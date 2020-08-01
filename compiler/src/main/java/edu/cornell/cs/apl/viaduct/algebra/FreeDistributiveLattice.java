package edu.cornell.cs.apl.viaduct.algebra;

import com.google.auto.value.AutoValue;
import io.vavr.collection.Array;
import io.vavr.collection.HashSet;
import io.vavr.collection.Set;
import java.util.function.Function;

/**
 * The free distributive lattice over an arbitrary set {@code A} of elements. In addition to lattice
 * identities, the following hold:
 *
 * <p>{@code a /\ (b \/ c) == (a /\ b) \/ (a /\ c)}
 *
 * <p>{@code a \/ (b /\ c) == (a \/ b) /\ (a \/ c)}
 */
@AutoValue
public abstract class FreeDistributiveLattice<A>
    implements HeytingAlgebra<FreeDistributiveLattice<A>> {
  private static final FreeDistributiveLattice<?> BOTTOM = create(HashSet.of());

  private static final FreeDistributiveLattice<?> TOP = create(HashSet.of(HashSet.of()));

  public static <A> FreeDistributiveLattice<A> create(A element) {
    return new AutoValue_FreeDistributiveLattice<>(HashSet.of(HashSet.of(element)));
  }

  private static <A> FreeDistributiveLattice<A> create(Set<Set<A>> joinOfMeets) {
    return new AutoValue_FreeDistributiveLattice<>(removeRedundant(joinOfMeets));
  }

  public static <A> FreeDistributiveLattice<A> top() {
    return (FreeDistributiveLattice<A>) TOP;
  }

  public static <A> FreeDistributiveLattice<A> bottom() {
    return (FreeDistributiveLattice<A>) BOTTOM;
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

  abstract Set<Set<A>> getJoinOfMeets();

  @Override
  public final boolean lessThanOrEqualTo(FreeDistributiveLattice<A> that) {
    return this.join(that).equals(that);
  }

  @Override
  public final FreeDistributiveLattice<A> join(FreeDistributiveLattice<A> with) {
    return create(this.getJoinOfMeets().union(with.getJoinOfMeets()));
  }

  @Override
  public final FreeDistributiveLattice<A> meet(FreeDistributiveLattice<A> with) {
    Set<Set<A>> candidates = HashSet.empty();

    for (Set<A> meet1 : this.getJoinOfMeets()) {
      for (Set<A> meet2 : with.getJoinOfMeets()) {
        candidates = candidates.add(meet1.union(meet2));
      }
    }

    return create(candidates);
  }

  /**
   * Returns the relative pseudocomplement of {@code this} relative to {@code that}. the relative
   * pseudocomplement is greatest x s.t. {@code this & x <= that}.
   *
   * <p>How does this work? we are dealing with constraints of the form {@code (A1 | ... | Am) & x
   * <= B1 | ... | Bn}
   *
   * <p>which can be rewritten as {@code (A1&x) | ... | (Am&x) <= B1 | ... | Bn}
   *
   * <p>This inequality only holds true if every meet on the left can be "covered" on the right s.t.
   * a meet on the right side is a subset of the meet in the left side. For every meet on the left
   * Ai, we complement it with every meet on the right Bj. because we want the greatest solution, we
   * join these complements together, arriving at an upper bound for x: {@code x <= Ci1 | ... | Cin}
   *
   * <p>where {@code Cij = Bj \ Ai}.
   *
   * <p>But we have to do the same process for all meets on the left, so we get m upper bounds.
   * these have to be all simultaneously satisfied, so we take the meet of the upper bounds: {@code
   * x = (C11 | ... | C1n) & ... & (Cm1 | ... | Cmn)}
   *
   * <p>The algorithm below computes exactly this solution.
   */
  @Override
  public final FreeDistributiveLattice<A> imply(FreeDistributiveLattice<A> that) {
    // TODO: make sure this is correct
    return this.getJoinOfMeets()
        .foldRight(
            top(),
            (thisMeet, acc) -> {
              Set<Set<A>> newJoinOfMeets =
                  that.getJoinOfMeets().map((thatMeet) -> thatMeet.removeAll(thisMeet));
              return acc.meet(create(newJoinOfMeets));
            });
  }

  @Override
  public String toString() {
    final Function<Set<A>, String> meetToString =
        (meet) -> {
          final Array<String> elements = meet.toArray().sorted().map(Object::toString);
          final String body = String.join(" \u2227 ", elements);
          return meet.length() > 1 ? "(" + body + ")" : body;
        };

    if (this.equals(top())) {
      return "\u22A4";
    } else if (this.equals(bottom())) {
      return "\u22A5";
    } else {
      final Array<String> meets = getJoinOfMeets().toArray().map(meetToString).sorted();
      final String body = String.join(" \u2228 ", meets);
      return getJoinOfMeets().length() > 1 ? "(" + body + ")" : body;
    }
  }
}
