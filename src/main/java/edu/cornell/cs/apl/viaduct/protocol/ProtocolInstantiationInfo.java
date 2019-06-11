package edu.cornell.cs.apl.viaduct.protocol;

import edu.cornell.cs.apl.viaduct.AstNode;
import edu.cornell.cs.apl.viaduct.Binding;
import edu.cornell.cs.apl.viaduct.imp.ast.Host;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.imp.builders.ProcessConfigurationBuilder;
import edu.cornell.cs.apl.viaduct.imp.builders.StmtBuilder;
import edu.cornell.cs.apl.viaduct.pdg.PdgNode;
import edu.cornell.cs.apl.viaduct.pdg.ProgramDependencyGraph.ControlLabel;

import java.util.Map;
import java.util.Set;
import java.util.Stack;

/** helper class for protocol instantiation. */
public class ProtocolInstantiationInfo<T extends AstNode> {
  private final ProcessConfigurationBuilder pconfig;
  private final Map<PdgNode<T>, Protocol<T>> protocolMap;
  private final Stack<Set<Host>> controlContext;

  /** store config builder and protocol map. */
  public ProtocolInstantiationInfo(
      ProcessConfigurationBuilder pc, Map<PdgNode<T>, Protocol<T>> pm) {

    this.pconfig = pc;
    this.protocolMap = pm;
    this.controlContext = new Stack<>();
  }

  public StmtBuilder createProcess(Host h) {
    this.pconfig.createProcess(h);
    return this.pconfig.getBuilder(h);
  }

  public Protocol<T> getProtocol(PdgNode<T> node) {
    return this.protocolMap.get(node);
  }

  public StmtBuilder getBuilder(Host h) {
    return this.pconfig.getBuilder(h);
  }

  public Variable getFreshVar(String base) {
    return this.pconfig.getFreshVar(base);
  }

  public Variable getFreshVar(Binding<T> base) {
    return getFreshVar(base.getBinding());
  }

  public String getFreshName(String base) {
    return this.pconfig.getFreshName(base);
  }

  public String getFreshName(Binding<T> base) {
    return getFreshName(base.getBinding());
  }

  public boolean isControlContextEmpty() {
    return this.controlContext.isEmpty();
  }

  public void pushControlContext(Set<Host> hosts) {
    this.controlContext.push(hosts);
  }

  /** set the current path for all hosts participating in the control structure. */
  public void setCurrentPath(ControlLabel label) {
    assert !this.controlContext.isEmpty();

    Set<Host> hosts = this.controlContext.peek();
    for (Host host : hosts) {
      StmtBuilder hostBuilder = this.pconfig.getBuilder(host);
      hostBuilder.setCurrentPath(label);
    }
  }

  /** finish the current execution path for all hosts participating in control structure. */
  public void finishCurrentPath() {
    assert !this.controlContext.isEmpty();

    Set<Host> hosts = this.controlContext.peek();
    for (Host host : hosts) {
      StmtBuilder hostBuilder = this.pconfig.getBuilder(host);
      hostBuilder.finishCurrentPath();
    }
  }

  /** pop the current control structure for all participating hosts. */
  public void popControl() {
    assert !this.controlContext.isEmpty();

    Set<Host> hosts = this.controlContext.peek();
    for (Host host : hosts) {
      StmtBuilder hostBuilder = this.pconfig.getBuilder(host);
      hostBuilder.popControl();
    }
  }
}
