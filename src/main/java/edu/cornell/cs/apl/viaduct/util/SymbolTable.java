package edu.cornell.cs.apl.viaduct.util;

import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import java.util.NoSuchElementException;
import java.util.Stack;

/** A generic data structure for a stack of maps, useful for maintaining lexical scoping. */
public class SymbolTable<V, T> {
  private final Stack<Map<V, T>> tableStack;

  public SymbolTable() {
    this.tableStack = new Stack<>();
    this.tableStack.push(HashMap.empty());
  }

  public boolean contains(V var) {
    return this.tableStack.peek().containsKey(var);
  }

  /** Get the value associated with the given in the current (i.e. topmost/innermost) scope. */
  public T get(V var) {
    return this.tableStack
        .peek()
        .get(var)
        .getOrElseThrow(() -> new NoSuchElementException(var.toString()));
  }

  public void add(V var, T data) {
    this.tableStack.push(this.tableStack.pop().put(var, data));
  }

  public void push() {
    this.tableStack.push(this.tableStack.peek());
  }

  public void pop() {
    this.tableStack.pop();
  }

  @Override
  public String toString() {
    return this.tableStack.peek().toString();
  }
}
