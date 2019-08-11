package edu.cornell.cs.apl.viaduct.util;

import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import java.util.NoSuchElementException;
import java.util.Stack;

/** A generic data structure for a stack of maps, useful for maintaining lexical scoping. */
public class SymbolTable<K, V> implements Cloneable {
  private final Stack<Map<K, V>> tableStack;

  public SymbolTable() {
    this.tableStack = new Stack<>();
    this.tableStack.push(HashMap.empty());
  }

  private SymbolTable(Stack<Map<K, V>> tableStack) {
    this.tableStack = tableStack;
  }

  public void clear() {
    this.tableStack.clear();
    this.tableStack.push(HashMap.empty());
  }

  public boolean contains(K key) {
    return this.tableStack.peek().containsKey(key);
  }

  /**
   * Get the value associated with the given key in the current (i.e. topmost/innermost) scope.
   *
   * @throws NoSuchElementException if the key is not in the table
   */
  public V get(K key) {
    return this.tableStack
        .peek()
        .get(key)
        .getOrElseThrow(() -> new NoSuchElementException(key.toString()));
  }

  /**
   * Add the given key with the given value to the current scope. Replaces the current mapping if
   * one exists.
   */
  public void put(K key, V value) {
    this.tableStack.push(this.tableStack.pop().put(key, value));
  }

  /** Add a new scope. The new scope contains all mappings in previous scopes. */
  public void push() {
    this.tableStack.push(this.tableStack.peek());
  }

  /** Remove the most recently added scope. */
  public void pop() {
    this.tableStack.pop();
  }

  // TODO: delete this
  @Override
  public SymbolTable<K, V> clone() throws CloneNotSupportedException {
    super.clone(); // Shut SpotBugs up
    Stack<Map<K, V>> newStack = new Stack<>();
    newStack.addAll(this.tableStack);
    return new SymbolTable<>(newStack);
  }

  @Override
  public String toString() {
    return this.tableStack.peek().toString();
  }
}
