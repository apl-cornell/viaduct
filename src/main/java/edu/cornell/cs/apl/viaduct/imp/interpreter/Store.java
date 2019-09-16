package edu.cornell.cs.apl.viaduct.imp.interpreter;

import com.google.auto.value.AutoValue;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.imp.ast.values.ImpValue;
import io.vavr.Tuple2;
import io.vavr.collection.Map;
import io.vavr.collection.Set;
import io.vavr.collection.SortedMap;
import io.vavr.collection.SortedSet;
import io.vavr.collection.TreeMap;
import java.io.PrintStream;

/**
 * The final state of a process' store. This is essentially a mapping from variables to objects they
 * refer to.
 *
 * <p>For now, this class does not give access to individual mappings; it only supports general
 * operations like printing and checking for equality.
 */
@AutoValue
public abstract class Store {
  static Store create(Map<Variable, AllocatedObject> store) {
    final Map<Variable, Object> values = store.mapValues(AllocatedObject::getImmutableValue);
    // Use a sorted map for easier to read output.
    return new AutoValue_Store(TreeMap.ofAll(values.toJavaMap()));
  }

  /**
   * Return the mapping from variables to their values.
   *
   * <p>Here, the values are either an {@link ImpValue} or an array of {@link ImpValue}s.
   */
  abstract SortedMap<Variable, Object> getMap();

  /** Return {@code true} if no variables are declared in the store. */
  public boolean isEmpty() {
    return getMap().isEmpty();
  }

  /** Return the set of variables in the store. */
  public SortedSet<Variable> variableSet() {
    return getMap().keySet();
  }

  /**
   * Check if the values in this store agree with the values in another when restricted to the given
   * set of variables.
   *
   * @param that store to compare to
   * @param variables restrict comparison to this set of variables
   */
  public boolean agreesWith(Store that, Set<Variable> variables) {
    final Map<Variable, Object> thisRestricted = getMap().filterKeys(variables::contains);
    final Map<Variable, Object> thatRestricted = that.getMap().filterKeys(variables::contains);
    return thisRestricted.equals(thatRestricted);
  }

  /** Pretty print (with colors) to the given output stream. */
  public void print(PrintStream output) {
    if (getMap().isEmpty()) {
      output.println("<empty>");
      return;
    }

    for (Tuple2<Variable, Object> entry : getMap()) {
      output.print(entry._1());
      output.print(" => ");
      output.print(entry._2());
      output.println();
    }
  }
}
