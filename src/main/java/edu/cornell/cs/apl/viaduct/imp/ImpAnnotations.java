package edu.cornell.cs.apl.viaduct.imp;

import edu.cornell.cs.apl.viaduct.imp.ast.ImpValue;
import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;

public class ImpAnnotations {
  /** specifies where a program is executed. */
  public static class ProcessAnnotation implements ImpAnnotation {
    String host;

    public ProcessAnnotation(String h) {
      this.host = h;
    }

    public String getHost() {
      return host;
    }
  }

  /** execute a statement in the interpreter that is not
   * visible to protocol synthesis. */
  public static class InterpAnnotation implements ImpAnnotation {
    StmtNode program;

    public InterpAnnotation(StmtNode p) {
      this.program = p;
    }

    public StmtNode getProgram() {
      return this.program;
    }
  }

  /** let process receive from INPUT channel. */
  public static class InputAnnotation implements ImpAnnotation {
    ImpValue value;

    public InputAnnotation(ImpValue v) {
      this.value = v;
    }

    public ImpValue getValue() {
      return this.value;
    }
  }
}
