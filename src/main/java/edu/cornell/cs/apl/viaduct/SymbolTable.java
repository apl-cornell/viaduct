package edu.cornell.cs.apl.viaduct;

import java.util.HashMap;
import java.util.Stack;

/** a generic data structure for a stack of maps, useful for maintaining lexical scoping. */
public class SymbolTable<V, T> {
  Stack<HashMap<V, T>> tableStack;

  public SymbolTable() {
    this.tableStack = new Stack<HashMap<V, T>>();
    this.tableStack.push(new HashMap<V, T>());
  }

  public boolean contains(V var) {
    return this.tableStack.peek().containsKey(var);
  }

  public T get(V var) {
    return this.tableStack.peek().get(var);
  }

  public void add(V var, T data) {
    this.tableStack.peek().put(var, data);
  }

  public void push() {
    HashMap<V, T> newTable = (HashMap<V, T>) this.tableStack.peek().clone();
    this.tableStack.push(newTable);
  }

  public void pop() {
    this.tableStack.pop();
  }

  public String toString() {
    return this.tableStack.peek().toString();
  }
}
