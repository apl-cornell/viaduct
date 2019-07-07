package edu.cornell.cs.apl.viaduct.imp.interpreter;

class BreakSignal extends RuntimeException {
  private final int level;

  public BreakSignal(int level) {
    this.level = level;
  }

  public int getLevel() {
    return this.level;
  }
}
