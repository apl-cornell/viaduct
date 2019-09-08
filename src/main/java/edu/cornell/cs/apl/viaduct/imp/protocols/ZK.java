package edu.cornell.cs.apl.viaduct.imp.protocols;

import edu.cornell.cs.apl.viaduct.Binding;
import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.HostName;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.pdg.PdgNode;
import edu.cornell.cs.apl.viaduct.protocol.Protocol;
import edu.cornell.cs.apl.viaduct.protocol.ProtocolInstantiationInfo;
import edu.cornell.cs.apl.viaduct.security.Label;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** non-interactive zero-knowledge proof. */
public class ZK implements Protocol<ImpAstNode> {
  private final HostName prover;
  private final HostName verifier;
  private final Label trust;

  /** constructor. */
  public ZK(HostTrustConfiguration hostConfig, HostName p, HostName v) {
    this.prover = p;
    this.verifier = v;
    this.trust = hostConfig.getTrust(p).and(hostConfig.getTrust(v).integrity());
  }

  @Override
  public String getId() {
    return "ZK";
  }

  @Override
  public Set<HostName> getHosts() {
    Set<HostName> hosts = new HashSet<>();
    hosts.add(this.prover);
    hosts.add(this.verifier);
    return hosts;
  }

  @Override
  public Label getTrust() {
    return this.trust;
  }

  @Override
  public void initialize(PdgNode<ImpAstNode> node, ProtocolInstantiationInfo<ImpAstNode> info) {}

  @Override
  public void instantiate(PdgNode<ImpAstNode> node, ProtocolInstantiationInfo<ImpAstNode> info) {

    // TODO: finish
  }

  @Override
  public Binding<ImpAstNode> readFrom(
      PdgNode<ImpAstNode> node,
      PdgNode<ImpAstNode> readNode,
      HostName readHost,
      Binding<ImpAstNode> readLabel,
      List<ImpAstNode> args,
      ProtocolInstantiationInfo<ImpAstNode> info) {

    // TODO: finish
    return null;
  }

  @Override
  public void writeTo(
      PdgNode<ImpAstNode> node,
      PdgNode<ImpAstNode> writeNode,
      HostName writeHost,
      List<ImpAstNode> args,
      ProtocolInstantiationInfo<ImpAstNode> info) {

    // TODO: finish
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }

    if (o instanceof ZK) {
      ZK ozk = (ZK) o;
      boolean peq = this.prover.equals(ozk.prover);
      boolean veq = this.prover.equals(ozk.prover);
      return peq && veq;
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.prover, this.verifier);
  }

  @Override
  public String toString() {
    String pname = this.prover.toString();
    String vname = this.verifier.toString();
    return String.format("ZK(%s,%s)", pname, vname);
  }
}
