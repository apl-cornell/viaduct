package edu.cornell.cs.apl.viaduct.imp.interpreter;

import edu.cornell.cs.apl.viaduct.imp.ast.ImpValue;
import edu.cornell.cs.apl.viaduct.imp.ast.UnavailableValue;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import io.vavr.Tuple2;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.annotation.Nonnull;

/** Maps variables to their values. */
public class Store implements Iterable<Tuple2<Variable, ImpValue>> {
  /** Maps variables to their values. */
  private final Map<Variable, ImpValue> variableStore = new HashMap<>();

  /** Maps array variables to Java arrays storing the contents. */
  private final Map<Variable, ImpValue[]> arrayStore = new HashMap<>();

  /** Create an empty store. */
  Store() {}

  /**
   * Declare a new variable. No value is associated with the variable, and attempting to read it
   * without assigning to it first will result in an exception.
   *
   * @param variable variable to declare
   * @throws RedeclaredVariableException if the variable is already declared.
   */
  void declare(Variable variable) throws RedeclaredVariableException {
    if (variableStore.containsKey(variable)) {
      throw new RedeclaredVariableException(variable);
    }
    this.variableStore.put(variable, new UnavailableValue());
  }

  /**
   * Return the value associated with a variable.
   *
   * @param variable the variable to lookup
   * @throws UndeclaredVariableException if the variable was not declared.
   * @throws UnassignedVariableException if the variable was never assigned a value.
   */
  @Nonnull
  ImpValue lookup(Variable variable)
      throws UndeclaredVariableException, UnassignedVariableException {
    final ImpValue value = variableStore.get(variable);

    if (value == null) {
      throw new UndeclaredVariableException(variable);
    } else if (value instanceof UnavailableValue) {
      throw new UnassignedVariableException(variable);
    } else {
      return variableStore.get(variable);
    }
  }

  /**
   * Update the value associated with a variable that is already declared.
   *
   * @param variable the variable whose value should be updated
   * @param newValue the new value to associate with the variable
   * @throws UndeclaredVariableException if the variable was not declared.
   */
  void update(Variable variable, ImpValue newValue) throws UndeclaredVariableException {
    if (!variableStore.containsKey(variable)) {
      throw new UndeclaredVariableException(variable);
    }
    variableStore.put(variable, newValue);
  }

  @Override
  public @Nonnull Iterator<Tuple2<Variable, ImpValue>> iterator() {
    return io.vavr.collection.HashMap.ofAll(variableStore).iterator();
  }

  @Override
  public String toString() {
    final StringBuilder buffer = new StringBuilder();

    boolean first = true;
    for (Map.Entry<Variable, ImpValue> entry : variableStore.entrySet()) {
      if (!first) {
        buffer.append("\n");
      }

      buffer.append(entry.getKey());
      buffer.append(" => ");
      buffer.append(entry.getValue());

      first = false;
    }

    return buffer.toString();
  }
}
