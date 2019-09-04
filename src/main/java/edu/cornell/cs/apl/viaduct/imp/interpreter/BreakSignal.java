package edu.cornell.cs.apl.viaduct.imp.interpreter;

final class BreakSignal extends RuntimeException {
  private final int level;

  BreakSignal(int level) {
    this.level = level;
  }

  int getLevel() {
    return this.level;
  }
}
