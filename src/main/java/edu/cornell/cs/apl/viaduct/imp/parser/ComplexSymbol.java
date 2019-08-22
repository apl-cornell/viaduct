package edu.cornell.cs.apl.viaduct.imp.parser;

import java_cup.runtime.Symbol;

/** A symbol that maintains a source location (the way we defined source locations). */
class ComplexSymbol extends Symbol implements Located {
  private final String name;
  private final SourceRange location;

  ComplexSymbol(String name, int id, SourceRange location, Object value) {
    super(id, location.getStart().getOffset(), location.getEnd().getOffset(), value);
    this.name = name;
    this.location = location;
  }

  ComplexSymbol(String name, int id, Object value) {
    super(id, value);
    this.name = name;
    this.location = null;
  }

  ComplexSymbol(String name, int id, int state) {
    super(id);
    this.parse_state = state;
    this.name = name;
    this.location = null;
  }

  public String getName() {
    return name;
  }

  @Override
  public SourceRange getSourceLocation() {
    return location;
  }

  @Override
  public String toString() {
    return "Symbol: " + getName() + (location != null ? " (" + getSourceLocation() + ")" : "");
  }
}
