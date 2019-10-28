package edu.cornell.cs.apl.viaduct.imp.protocols;

import edu.cornell.cs.apl.viaduct.Binding;
import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.HostName;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessName;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.imp.builders.ExpressionBuilder;
import edu.cornell.cs.apl.viaduct.imp.builders.StmtBuilder;
import edu.cornell.cs.apl.viaduct.pdg.PdgComputeNode;
import edu.cornell.cs.apl.viaduct.pdg.PdgNode;
import edu.cornell.cs.apl.viaduct.pdg.PdgStorageNode;
import edu.cornell.cs.apl.viaduct.protocol.Protocol;
import edu.cornell.cs.apl.viaduct.protocol.ProtocolInstantiationError;
import edu.cornell.cs.apl.viaduct.protocol.ProtocolInstantiationInfo;
import edu.cornell.cs.apl.viaduct.security.Label;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** replication protocol. */
public class Replication extends Cleartext implements Protocol<ImpAstNode> {
  private final Label trust;
  private final Map<ProcessName, Variable> outVarMap;

  /** constructor. */
  public Replication(HostTrustConfiguration hostConfig, Set<HostName> hosts) {
    super(hosts);

    this.outVarMap = new HashMap<>();
    Label label = Label.top();
    for (HostName host : this.hosts) {
      label = label.meet(hostConfig.getTrust(host));
    }
    this.trust = label;
  }

  @Override
  public String getId() {
    return "Replication";
  }

  @Override
  public Set<ProcessName> getProcesses() {
    Set<ProcessName> processes = new HashSet<>();
    for (HostName host : this.hosts) {
      processes.add(ProcessName.create(host));
    }

    return processes;
  }

  @Override
  public boolean hasSyntheticProcesses() {
    return false;
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
      for (ProcessName realProcess : getProcesses()) {
        Variable hostStorageVar =
            instantiateStorageNode(realProcess, (PdgStorageNode<ImpAstNode>) node, info);
        this.outVarMap.put(realProcess, hostStorageVar);
      }

    } else if (node.isComputeNode()) {
      for (ProcessName realProcess : getProcesses()) {
        Variable hostOutVar =
            instantiateComputeNode(realProcess, (PdgComputeNode<ImpAstNode>) node, info);
        this.outVarMap.put(realProcess, hostOutVar);
      }

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

    // should not be read from until it has been instantiated
    assert this.outVarMap.size() == this.hosts.size();

    Map<ProcessName, Binding<ImpAstNode>> processBindings = new HashMap<>();
    StmtBuilder readBuilder = info.getBuilder(readProcess);
    Set<ProcessName> outProcesses = info.getReadSet(node, readNode, readProcess);

    for (ProcessName outProcess : outProcesses) {
      Variable outVar = this.outVarMap.get(outProcess);
      Binding<ImpAstNode> readVar =
          performRead(node, readNode, readProcess, readLabel, outProcess, outVar, args, info);
      processBindings.put(outProcess, readVar);
    }

    if (processBindings.size() > 1) {
      Binding<ImpAstNode> curBinding = null;
      ExpressionNode curExpr = null;
      ExpressionBuilder e = new ExpressionBuilder();
      for (Binding<ImpAstNode> binding : processBindings.values()) {
        if (curBinding == null) {
          curBinding = binding;

        } else {
          if (curExpr == null) {
            curExpr = e.equals(e.var(curBinding), e.var(binding));
            curBinding = binding;

          } else {
            curExpr = e.and(curExpr, e.equals(e.var(curBinding), e.var(binding)));
            curBinding = binding;
          }
        }
      }

      readBuilder.assertion(curExpr);
    }

    ProcessName process = (ProcessName) processBindings.keySet().toArray()[0];
    return processBindings.get(process);
  }

  @Override
  public void writeTo(
      PdgNode<ImpAstNode> node,
      PdgNode<ImpAstNode> writeNode,
      ProcessName writeProcess,
      List<ImpAstNode> args,
      ProtocolInstantiationInfo<ImpAstNode> info)
  {
    if (node.isStorageNode()) {
      // node must have been instantiated before being written to
      assert this.outVarMap.size() == this.hosts.size();

      Set<ProcessName> inProcesses = info.getWriteSet(writeNode, node, writeProcess);
      for (ProcessName inProcess : inProcesses) {
        Variable storageVar = this.outVarMap.get(inProcess);
        performWrite(node, writeNode, writeProcess, inProcess, storageVar, args, info);
      }

    } else {
      throw new ProtocolInstantiationError("attempted to write to a non storage node");
    }
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }

    if (o instanceof Replication) {
      Replication other = (Replication) o;
      boolean realEq = this.hosts.equals(other.hosts);
      return realEq;

    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.hosts);
  }
}
