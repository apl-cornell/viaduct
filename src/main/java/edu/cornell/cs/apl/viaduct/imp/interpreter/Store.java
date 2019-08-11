package edu.cornell.cs.apl.viaduct.imp.interpreter;

import edu.cornell.cs.apl.viaduct.imp.RedeclaredVariableException;
import edu.cornell.cs.apl.viaduct.imp.UndeclaredVariableException;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpValue;
import edu.cornell.cs.apl.viaduct.imp.ast.UnavailableValue;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.util.SymbolTable;
import io.vavr.Tuple2;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;

/** Maps variables to their values. */
public class Store implements Iterable<Tuple2<Variable, ImpValue>> {
  /** Maps variables to their values. */
  private final Map<Variable, ImpValue> variableStore = new HashMap<>();
  /** Maps array variables to Java arrays storing the contents. */
  private final Map<Variable, ImpValue[]> arrayStore = new HashMap<>();
  /** Maps temporaries to values. */
  private SymbolTable<Variable, ImpValue> tempStore = new SymbolTable<>();

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
      // might be a temporary; check the temp store
      return this.tempStore.get(variable);

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
   * Declare a new array with the given length. No value is associated to any index, and attempting
   * to access an index before assigning a value will throw an exception.
   *
   * @param variable variable naming the new array
   * @param length length of the array
   */
  void declareArray(Variable variable, int length) throws RedeclaredVariableException {
    if (this.arrayStore.containsKey(variable)) {
      throw new RedeclaredVariableException(variable);
    }
    ImpValue[] array = new ImpValue[length];
    Arrays.fill(array, UnavailableValue.create());
    this.arrayStore.put(variable, array);
  }

  /**
   * Return the value of an array at a particular index.
   *
   * @param variable the array variable
   * @param index the index to lookup
   * @throws UndeclaredVariableException if the array was not declared.
   * @throws UnassignedVariableException if the array was never assigned a value at the index.
   * @throws ImpArrayOutOfBoundsException if the index lies outside the array bounds.
   */
  ImpValue lookupArray(Variable variable, int index)
      throws UndeclaredVariableException, UnassignedVariableException,
          ImpArrayOutOfBoundsException {
    final ImpValue[] array = this.arrayStore.get(variable);

    try {
      if (array == null) {
        throw new UndeclaredVariableException(variable);
      } else if (array[index] instanceof UnavailableValue) {
        throw new UnassignedVariableException(variable);
      } else {
        return array[index];
      }
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new ImpArrayOutOfBoundsException(variable, index);
    }
  }

  /**
   * Update the value of an array at a particular index.
   *
   * @param variable the array whose value should be updated
   * @param index the index at which the array should be updated
   * @param newValue the new value to associate with the index
   * @throws UndeclaredVariableException if the variable was not declared.
   * @throws ImpArrayOutOfBoundsException if the index lies outside the array bounds.
   */
  void updateArray(Variable variable, int index, ImpValue newValue)
      throws UndeclaredVariableException, ImpArrayOutOfBoundsException {
    final ImpValue[] array = this.arrayStore.get(variable);

    if (array == null) {
      throw new UndeclaredVariableException(variable);
    }

    try {
      array[index] = newValue;
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new ImpArrayOutOfBoundsException(variable, index);
    }
  }

  /** push new context for temporary stores. */
  void pushTempContext() {
    this.tempStore.push();
  }

  void popTempContext() {
    this.tempStore.pop();
  }

  /**
   * Declare a new temporary variable.
   *
   * @param var temp variable to declare
   * @param val value of temporary
   */
  void declareTemp(Variable var, ImpValue val) throws RedeclaredVariableException {
    if (this.tempStore.contains(var)) {
      throw new RedeclaredVariableException(var);
    }
    this.tempStore.put(var, val);
  }

  /**
   * Lookup a temporary variable.
   *
   * @param var the temporary variable to lookup
   * @throws UndeclaredVariableException if the variable was not declared.
   */
  @Nonnull
  ImpValue lookupTemp(Variable var) {
    if (this.tempStore.contains(var)) {
      return this.tempStore.get(var);
    }
    throw new UndeclaredVariableException(var);
  }

  /** Save a temp table (used by non-local control structures). */
  SymbolTable<Variable, ImpValue> saveTempStore() {
    try {
      return (SymbolTable<Variable, ImpValue>) this.tempStore.clone();

    } catch (CloneNotSupportedException exn) { // impossible

    }

    // impossible to reach
    return null;
  }

  /**
   * Restore a saved temp table.
   *
   * @param tempStore the temp table to restore
   */
  void restoreTempStore(SymbolTable<Variable, ImpValue> tempStore) {
    this.tempStore = tempStore;
  }

  @Override
  public @Nonnull Iterator<Tuple2<Variable, ImpValue>> iterator() {
    return io.vavr.collection.HashMap.ofAll(variableStore).iterator();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof Store)) {
      return false;
    }

    Store that = (Store) o;

    // TODO: this is almost certainly broken because it doesn't match hashCode.
    //  also, there are functions for this.
    // arrays use == for equals(), so we need to compare the values manually
    if (this.arrayStore.size() == that.arrayStore.size()) {
      for (Map.Entry<Variable, ImpValue[]> kv : this.arrayStore.entrySet()) {
        if (that.arrayStore.containsKey(kv.getKey())) {
          ImpValue[] thatVal = that.arrayStore.get(kv.getKey());
          if (!Arrays.equals(kv.getValue(), thatVal)) {
            return false;
          }

        } else {
          return false;
        }
      }
    } else {
      return false;
    }

    return Objects.equals(this.variableStore, that.variableStore);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.variableStore, this.arrayStore);
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
