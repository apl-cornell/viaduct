package edu.cornell.cs.apl.viaduct.protocol;

import edu.cornell.cs.apl.viaduct.AstNode;
import edu.cornell.cs.apl.viaduct.Binding;
import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessName;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.imp.builders.ProcessConfigurationBuilder;
import edu.cornell.cs.apl.viaduct.imp.builders.StmtBuilder;
import edu.cornell.cs.apl.viaduct.pdg.PdgNode;
import edu.cornell.cs.apl.viaduct.pdg.ProgramDependencyGraph.ControlLabel;
import io.vavr.collection.Map;
import java.util.Set;
import java.util.Stack;

/** helper class for protocol instantiation. */
public class ProtocolInstantiationInfo<T extends AstNode> {
  private final HostTrustConfiguration hostConfig;
  private final ProtocolCommunicationStrategy<T> communicationStrategy;
  private final ProcessConfigurationBuilder<T> pconfig;
  private final Map<PdgNode<T>, Protocol<T>> protocolMap;
  private final Stack<Set<ProcessName>> controlContext;
  private final Stack<Set<ProcessName>> loopControlContext;

  /** store config builder and protocol map. */
  public ProtocolInstantiationInfo(
      HostTrustConfiguration hostConfig,
      ProtocolCommunicationStrategy<T> communicationStrategy,
      ProcessConfigurationBuilder<T> processConfig,
      Map<PdgNode<T>, Protocol<T>> protocolMap) {

    this.hostConfig = hostConfig;
    this.communicationStrategy = communicationStrategy;
    this.pconfig = processConfig;
    this.protocolMap = protocolMap;
    this.controlContext = new Stack<>();
    this.loopControlContext = new Stack<>();
  }

  /** get the set of hosts to read from. */
  public Set<ProcessName> getReadSet(
      PdgNode<T> writeNode, PdgNode<T> readNode, ProcessName process) {
    final Protocol<T> fromProtocol = this.protocolMap.getOrElse(writeNode, null);
    final Protocol<T> toProtocol = this.protocolMap.getOrElse(readNode, null);
    return this.communicationStrategy.getReadSet(
        this.hostConfig, fromProtocol, toProtocol, process);
  }

  /** get the set of hosts to write to. */
  public Set<ProcessName> getWriteSet(
      PdgNode<T> writeNode, PdgNode<T> readNode, ProcessName process) {
    final Protocol<T> fromProtocol = this.protocolMap.getOrElse(writeNode, null);
    final Protocol<T> toProtocol = this.protocolMap.getOrElse(readNode, null);
    return this.communicationStrategy.getWriteSet(
        this.hostConfig, fromProtocol, toProtocol, process);
  }

  public StmtBuilder createProcess(ProcessName process) {
    this.pconfig.createProcess(process);
    return this.pconfig.getBuilder(process);
  }

  public StmtBuilder createProcess(ProcessName process, Protocol<T> protocol) {
    this.pconfig.createProcess(process, protocol);
    return this.pconfig.getBuilder(process);
  }

  public Protocol<T> getProtocol(PdgNode<T> node) {
    return this.protocolMap.getOrElse(node, null);
  }

  public StmtBuilder getBuilder(ProcessName process) {
    return this.pconfig.getBuilder(process);
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

  public boolean isLoopControlContextEmpty() {
    return this.loopControlContext.isEmpty();
  }

  /** get hosts in loop control context. */
  public Set<ProcessName> getCurrentLoopControlContext() {
    if (!this.loopControlContext.isEmpty()) {
      return this.loopControlContext.peek();

    } else {
      throw new ProtocolInstantiationError("attempting to peek empty loop control context stack");
    }
  }

  public void pushLoopControlContext(Set<ProcessName> processes) {
    this.loopControlContext.push(processes);
  }

  public void popLoopControlContext() {
    this.loopControlContext.pop();
  }

  public boolean isControlContextEmpty() {
    return this.controlContext.isEmpty();
  }

  /** get the hosts participating in the control context. */
  public Set<ProcessName> getCurrentControlContext() {
    if (!this.controlContext.isEmpty()) {
      return this.controlContext.peek();

    } else {
      throw new ProtocolInstantiationError("attempting to peek empty control context stack");
    }
  }

  public void pushControlContext(Set<ProcessName> processes) {
    this.controlContext.push(processes);
  }

  /** set the current path for all hosts participating in the control structure. */
  public void setCurrentPath(ControlLabel label) {
    assert !this.controlContext.isEmpty();

    Set<ProcessName> processes = this.controlContext.peek();
    for (ProcessName process : processes) {
      StmtBuilder hostBuilder = this.pconfig.getBuilder(process);
      hostBuilder.setCurrentPath(label);
    }
  }

  /** finish the current execution path for all hosts participating in control structure. */
  public void finishCurrentPath() {
    assert !this.controlContext.isEmpty();

    Set<ProcessName> processes = this.controlContext.peek();
    for (ProcessName process : processes) {
      StmtBuilder hostBuilder = this.pconfig.getBuilder(process);
      hostBuilder.finishCurrentPath();
    }
  }

  /** pop the current control structure for all participating hosts. */
  public void popControlContext() {
    assert !this.controlContext.isEmpty();

    Set<ProcessName> processes = this.controlContext.peek();
    for (ProcessName process : processes) {
      StmtBuilder hostBuilder = this.pconfig.getBuilder(process);
      hostBuilder.popControl();
    }
    this.controlContext.pop();
  }
}
