package edu.cornell.cs.apl.viaduct.imp.protocols;

import edu.cornell.cs.apl.viaduct.Binding;
import edu.cornell.cs.apl.viaduct.PdgComputeNode;
import edu.cornell.cs.apl.viaduct.PdgNode;
import edu.cornell.cs.apl.viaduct.Protocol;
import edu.cornell.cs.apl.viaduct.ProtocolInstantiationInfo;
import edu.cornell.cs.apl.viaduct.imp.ast.Host;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.imp.builders.ExpressionBuilder;
import edu.cornell.cs.apl.viaduct.imp.builders.StmtBuilder;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** multiparty computation protocol. */
public class MPC extends Cleartext implements Protocol<ImpAstNode> {
  private Set<Host> parties;
  private Host synthesizedHost;
  private Variable outVar;

  public MPC(Set<Host> ps) {
    this.parties = ps;
  }

  @Override
  public Set<Host> getHosts() {
    Set<Host> hosts = new HashSet<>();
    hosts.addAll(this.parties);
    return hosts;
  }

  @Override
  public Set<Host> readFrom(PdgNode<ImpAstNode> node, Host readHost,
      ProtocolInstantiationInfo<ImpAstNode> info) {

    // this should not be read from until it has been instantiated!
    assert this.outVar != null;

    ExpressionBuilder e = new ExpressionBuilder();
    StmtBuilder builder = info.getBuilder(this.synthesizedHost);
    builder.send(readHost, e.var(this.outVar));

    Set<Host> hosts = new HashSet<>();
    hosts.add(this.synthesizedHost);
    return hosts;
  }

  @Override
  public Binding<ImpAstNode> readPostprocess(Map<Host, Binding<ImpAstNode>> hostBindings,
      Host host, ProtocolInstantiationInfo<ImpAstNode> info) {

    // because this is the Single protocol, there should only
    // have been one host that the node read from
    assert hostBindings.size() == 1;
    return hostBindings.get(this.synthesizedHost);
  }

  @Override
  public void writeTo(PdgNode<ImpAstNode> node, Host h, ImpAstNode val,
      ProtocolInstantiationInfo<ImpAstNode> info) {

    // MPC is only for computations, so it cannot be written to!
    // do nothing here.
  }

  @Override
  public void instantiate(PdgNode<ImpAstNode> node, ProtocolInstantiationInfo<ImpAstNode> info) {
    this.synthesizedHost = new Host(info.getFreshName(toString()));
    info.createProcess(this.synthesizedHost);
    this.outVar =
        instantiateComputeNode(this.synthesizedHost, (PdgComputeNode<ImpAstNode>) node, info);
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }

    if (o instanceof MPC) {
      MPC ompc = (MPC) o;
      return this.parties.equals(ompc.parties);

    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return this.parties.hashCode();
  }

  @Override
  public String toString() {
    HashSet<String> strs = new HashSet<>();
    for (Host party : this.parties) {
      strs.add(party.toString());
    }

    String strList = String.join(",", strs);
    return String.format("MPC({%s})", strList);
  }
}
