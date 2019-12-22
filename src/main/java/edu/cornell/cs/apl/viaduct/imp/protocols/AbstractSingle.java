package edu.cornell.cs.apl.viaduct.imp.protocols;

import edu.cornell.cs.apl.viaduct.Binding;
import edu.cornell.cs.apl.viaduct.imp.ast.HostName;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessName;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.pdg.PdgComputeNode;
import edu.cornell.cs.apl.viaduct.pdg.PdgNode;
import edu.cornell.cs.apl.viaduct.pdg.PdgStorageNode;
import edu.cornell.cs.apl.viaduct.protocol.ProtocolInstantiationError;
import edu.cornell.cs.apl.viaduct.protocol.ProtocolInstantiationInfo;
import edu.cornell.cs.apl.viaduct.security.Label;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class AbstractSingle extends Cleartext {
  protected Variable outVar;
  protected ProcessName process;
  protected final Label trust;

  protected AbstractSingle(Set<HostName> hosts, Label trust) {
    super(hosts);
    this.trust = trust;
  }

  protected AbstractSingle(HostName host, Label trust) {
    super(host);
    this.trust = trust;
  }

  public ProcessName getProcess() {
    return this.process;
  }

  @Override
  public Set<ProcessName> getProcesses() {
    Set<ProcessName> processes = new HashSet<>();
    processes.add(this.process);
    return processes;
  }

  @Override
  public Label getTrust() {
    return this.trust;
  }

  @Override
  public void initialize(PdgNode<ImpAstNode> node, ProtocolInstantiationInfo<ImpAstNode> info) {
    return;
  }

  @Override
  public void instantiate(PdgNode<ImpAstNode> node, ProtocolInstantiationInfo<ImpAstNode> info) {
    if (node.isStorageNode()) {
      this.outVar = instantiateStorageNode(this.process, (PdgStorageNode<ImpAstNode>) node, info);

    } else if (node.isComputeNode()) {
      this.outVar = instantiateComputeNode(this.process, (PdgComputeNode<ImpAstNode>) node, info);

    } else {
      throw new ProtocolInstantiationError("control nodes must have Control protocol");
    }
  }

  @Override
  public Binding<ImpAstNode> readFrom(
      PdgNode<ImpAstNode> node,
      PdgNode<ImpAstNode> readNode,
      ProcessName readProcess,
      Binding<ImpAstNode> readLabel,
      List<ImpAstNode> args,
      ProtocolInstantiationInfo<ImpAstNode> info) {

    // this should not be read from until it has been instantiated!
    assert this.outVar != null;
    return performRead(
        node, readNode, readProcess, readLabel, this.process, this.outVar, args, info);
  }

  @Override
  public void writeTo(
      PdgNode<ImpAstNode> node,
      PdgNode<ImpAstNode> writeNode,
      ProcessName writeProcess,
      List<ImpAstNode> args,
      ProtocolInstantiationInfo<ImpAstNode> info) {

    // can only write to storage nodes
    if (node.isStorageNode()) {
      // node must have been instantiated before being written to
      assert this.outVar != null;
      performWrite(node, writeNode, writeProcess, this.process, this.outVar, args, info);

    } else {
      throw new ProtocolInstantiationError("attempted to write to a non storage node");
    }
  }
}
