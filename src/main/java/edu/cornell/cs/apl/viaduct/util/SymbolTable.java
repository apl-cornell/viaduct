package edu.cornell.cs.apl.viaduct.util;

import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import java.util.NoSuchElementException;
import java.util.Stack;

/** A generic data structure for a stack of maps, useful for maintaining lexical scoping. */
public final class SymbolTable<K, V> implements Cloneable {
  private final Stack<Map<K, V>> tableStack;

  private SymbolTable(Stack<Map<K, V>> tableStack) {
    this.tableStack = tableStack;
  }

  public SymbolTable() {
    this.tableStack = new Stack<>();
    this.tableStack.push(HashMap.empty());
  }

  public void clear() {
    this.tableStack.clear();
    this.tableStack.push(HashMap.empty());
  }

  public boolean contains(K key) {
    return this.tableStack.peek().containsKey(key);
  }

  /** Get the value associated with the given key in the current (i.e. topmost/innermost) scope. */
  public V get(K key) {
    return this.tableStack
        .peek()
        .get(key)
        .getOrElseThrow(() -> new NoSuchElementException(key.toString()));
  }

  /** Add the given key with the given value to the current scope. */
  public void add(K key, V value) {
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

  @Override
  public Object clone() throws CloneNotSupportedException {
    Stack<Map<K, V>> tableStack = (Stack<Map<K, V>>) this.tableStack.clone();
    return new SymbolTable<>(tableStack);
  }

  @Override
  public String toString() {
    return this.tableStack.peek().toString();
  }
}
