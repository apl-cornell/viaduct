package edu.cornell.cs.apl.viaduct.imp;

import edu.cornell.cs.apl.viaduct.Host;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpValue;
import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;
import edu.cornell.cs.apl.viaduct.imp.visitors.PrintVisitor;

public class ImpAnnotations {
  /** specifies where a program is executed. */
  public static class ProcessAnnotation extends ImpAnnotation {
    Host host;

    public ProcessAnnotation(Host h) {
      this.host = h;
    }

    public Host getHost() {
      return host;
    }

    public String getKeyword() {
      return "process";
    }

    protected String argsToString() {
      return this.host.toString();
    }
  }

  /** execute a statement in the interpreter that is not visible to protocol synthesis. */
  public static class InterpAnnotation extends ImpAnnotation {
    StmtNode program;

    public InterpAnnotation(StmtNode p) {
      this.program = p;
    }

    public StmtNode getProgram() {
      return this.program;
    }

    public String getKeyword() {
      return "interp";
    }

    protected String argsToString() {
      PrintVisitor printer = new PrintVisitor();
      String progStr = this.program.accept(printer);
      return progStr;
    }
  }

  /** let process receive from INPUT channel. */
  public static class InputAnnotation extends ImpAnnotation {
    ImpValue value;

    public InputAnnotation(ImpValue v) {
      this.value = v;
    }

    public ImpValue getValue() {
      return this.value;
    }

    public String getKeyword() {
      return "input";
    }

    protected String argsToString() {
      return this.value.toString();
    }
  }
}
