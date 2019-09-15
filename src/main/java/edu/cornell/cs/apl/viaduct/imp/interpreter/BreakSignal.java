package edu.cornell.cs.apl.viaduct.imp.interpreter;

import edu.cornell.cs.apl.viaduct.imp.ast.JumpLabel;
import javax.annotation.Nullable;

final class BreakSignal extends RuntimeException {
  private final JumpLabel label;

  BreakSignal(@Nullable JumpLabel label) {
    this.label = label;
  }

  @Nullable
  JumpLabel getLabel() {
    return this.label;
  }
}
