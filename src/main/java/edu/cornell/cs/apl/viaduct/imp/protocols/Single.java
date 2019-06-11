package edu.cornell.cs.apl.viaduct.imp.protocols;

import edu.cornell.cs.apl.viaduct.Binding;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Host;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessName;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.imp.builders.ExpressionBuilder;
import edu.cornell.cs.apl.viaduct.imp.builders.StmtBuilder;
import edu.cornell.cs.apl.viaduct.pdg.PdgComputeNode;
import edu.cornell.cs.apl.viaduct.pdg.PdgControlNode;
import edu.cornell.cs.apl.viaduct.pdg.PdgNode;
import edu.cornell.cs.apl.viaduct.pdg.PdgStorageNode;
import edu.cornell.cs.apl.viaduct.protocol.Protocol;
import edu.cornell.cs.apl.viaduct.protocol.ProtocolInstantiationInfo;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Single extends Cleartext implements Protocol<ImpAstNode> {
  private Host host;
  private Variable outVar;

  public Single(Host h) {
    this.host = h;
  }

  public Host getHost() {
    return this.host;
  }

  @Override
  public Set<Host> getHosts() {
    Set<Host> hosts = new HashSet<>();
    hosts.add(this.host);
    return hosts;
  }

  @Override
  public Set<Host> readFrom(
      PdgNode<ImpAstNode> node, Host readHost, ProtocolInstantiationInfo<ImpAstNode> info) {

    // this should not be read from until it has been instantiated!
    assert this.outVar != null;

    ExpressionBuilder e = new ExpressionBuilder();
    StmtBuilder builder = info.getBuilder(this.host);
    builder.send(new ProcessName(readHost), e.var(this.outVar));

    Set<Host> hosts = new HashSet<>();
    hosts.add(this.host);
    return hosts;
  }

  @Override
  public Binding<ImpAstNode> readPostprocess(
      Map<Host, Binding<ImpAstNode>> hostBindings,
      Host host,
      ProtocolInstantiationInfo<ImpAstNode> info) {

    // because this is the Single protocol, there should only
    // have been one host that the node read from
    assert hostBindings.size() == 1;
    return hostBindings.get(this.host);
  }

  @Override
  public void writeTo(
      PdgNode<ImpAstNode> node,
      Host writeHost,
      ImpAstNode val,
      ProtocolInstantiationInfo<ImpAstNode> info) {

    // can only write to storage nodes
    if (node.isStorageNode()) {
      // node must have been instantiated before being written to
      assert this.outVar != null;

      StmtBuilder builder = info.getBuilder(this.host);
      StmtBuilder writerBuilder = info.getBuilder(writeHost);

      writerBuilder.send(new ProcessName(this.host), (ExpressionNode) val);
      builder.recv(new ProcessName(writeHost), this.outVar);
    }
  }

  @Override
  public void instantiate(PdgNode<ImpAstNode> node, ProtocolInstantiationInfo<ImpAstNode> info) {
    if (node.isStorageNode()) {
      this.outVar = instantiateStorageNode(this.host, (PdgStorageNode<ImpAstNode>) node, info);

    } else if (node.isComputeNode()) {
      this.outVar = instantiateComputeNode(this.host, (PdgComputeNode<ImpAstNode>) node, info);

    } else if (node.isControlNode()) {
      instantiateControlNode(getHosts(), (PdgControlNode<ImpAstNode>) node, info);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }

    if (o instanceof Single) {
      Single osingle = (Single) o;
      return this.host.equals(osingle.host);

    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return this.host.hashCode();
  }

  @Override
  public String toString() {
    return String.format("Single(%s)", this.host.toString());
  }
}
