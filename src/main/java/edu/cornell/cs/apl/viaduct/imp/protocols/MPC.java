package edu.cornell.cs.apl.viaduct.imp.protocols;

import edu.cornell.cs.apl.viaduct.Binding;
import edu.cornell.cs.apl.viaduct.imp.ast.Host;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.pdg.PdgComputeNode;
import edu.cornell.cs.apl.viaduct.pdg.PdgNode;
import edu.cornell.cs.apl.viaduct.protocol.Protocol;
import edu.cornell.cs.apl.viaduct.protocol.ProtocolInstantiationException;
import edu.cornell.cs.apl.viaduct.protocol.ProtocolInstantiationInfo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** multiparty computation protocol. */
public class MPC extends Cleartext implements Protocol<ImpAstNode> {
  private static Map<Set<Host>,Host> synthesizedHostMap = new HashMap<>();
  private Set<Host> parties;
  private Host synthesizedHost;
  private Variable outVar;

  public MPC(Set<Host> ps) {
    this.parties = ps;
  }

  public Set<Host> getParties() {
    return this.parties;
  }

  @Override
  public Set<Host> getHosts() {
    Set<Host> hosts = new HashSet<>();
    hosts.add(this.synthesizedHost);
    return hosts;
  }

  @Override
  public void initialize(PdgNode<ImpAstNode> node, ProtocolInstantiationInfo<ImpAstNode> info) {
    if (synthesizedHostMap.containsKey(this.parties)) {
      this.synthesizedHost = synthesizedHostMap.get(this.parties);

    } else {
      this.synthesizedHost = new Host(info.getFreshName(toString()));
      synthesizedHostMap.put(this.parties, this.synthesizedHost);
      info.createProcess(this.synthesizedHost);
    }
  }

  @Override
  public void instantiate(PdgNode<ImpAstNode> node, ProtocolInstantiationInfo<ImpAstNode> info) {
    this.outVar =
        instantiateComputeNode(this.synthesizedHost, (PdgComputeNode<ImpAstNode>) node, info);
  }

  @Override
  public Binding<ImpAstNode> readFrom(
      PdgNode<ImpAstNode> node, Host readHost,
      Binding<ImpAstNode> readLabel, List<ImpAstNode> args,
      ProtocolInstantiationInfo<ImpAstNode> info) {

    // this should not be read from until it has been instantiated!
    assert this.outVar != null;
    return performRead(node, readHost, readLabel,
        this.synthesizedHost, this.outVar, args, info);
  }

  @Override
  public void writeTo(
      PdgNode<ImpAstNode> node,
      Host h,
      List<ImpAstNode> args,
      ProtocolInstantiationInfo<ImpAstNode> info) {

    // MPC is only for computations, so it cannot be written to!
    // do nothing here.
    throw new ProtocolInstantiationException("MPC protocol cannot be written to!");
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
    return String.format("MPC(%s)", strList);
  }
}
