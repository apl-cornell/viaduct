package edu.cornell.cs.apl.viaduct.imp.interpreter;

import edu.cornell.cs.apl.viaduct.imp.ast.ImpValue;
import edu.cornell.cs.apl.viaduct.imp.ast.UnavailableValue;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import io.vavr.Tuple2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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

  /** Return {@code true} if no variables are declared in the store. */
  public boolean isEmpty() {
    return variableStore.isEmpty();
  }

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
    this.variableStore.put(variable, UnavailableValue.create());
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
      return value;
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

  /**
   * declare a new array.
   *
   * @param var the variable to associate with the new array.
   * @param length the length of the array.
   */
  void declareArray(Variable var, int length) throws RedeclaredVariableException {
    if (this.arrayStore.containsKey(var)) {
      throw new RedeclaredVariableException(var);
    }
    ImpValue[] array = new ImpValue[length];
    this.arrayStore.put(var, array);
  }

  /**
   * return value of array at a particular index.
   *
   * @param var the array variable
   * @param index the index to lookup
   * @throws UndeclaredVariableException if the array variable was not declared.
   * @throws UnassignedVariableException if the array was never assigned a value at the index.
   * */
  ImpValue lookupArray(Variable var, int index)
      throws UndeclaredVariableException, ImpArrayOutOfBoundsException,
          UnassignedVariableException
  {
    final ImpValue[] array = this.arrayStore.get(var);
    if (array == null) {
      throw new UndeclaredVariableException(var);

    } else if (index < 0 || index >= array.length) {
      throw new ImpArrayOutOfBoundsException(var, index);

    } else if (array[index] instanceof UnavailableValue) {
      throw new UnassignedVariableException(var);

    } else {
      return array[index];
    }
  }

  void updateArray(Variable var, int index, ImpValue val)
      throws UndeclaredVariableException
  {
    final ImpValue[] array = this.arrayStore.get(var);
    if (array == null) {
      throw new UndeclaredVariableException(var);

    } else {
      array[index] = val;
    }
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

    for (Map.Entry<Variable, ImpValue[]> entry : arrayStore.entrySet()) {
      if (!first) {
        buffer.append("\n");
      }

      buffer.append(entry.getKey());
      buffer.append(" => ");
      buffer.append("{");
      List<String> arrayStr = new ArrayList<>();
      for (ImpValue val : entry.getValue()) {
        if (val != null) {
          arrayStr.add(val.toString());
        }
      }
      buffer.append(String.join(", ", arrayStr));
      buffer.append("}");

      first = false;
    }

    return buffer.toString();
  }
}
