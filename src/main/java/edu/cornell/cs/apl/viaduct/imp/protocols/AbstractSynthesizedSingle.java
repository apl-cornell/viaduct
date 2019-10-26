package edu.cornell.cs.apl.viaduct.imp.protocols;

import edu.cornell.cs.apl.viaduct.imp.ast.HostName;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessName;
import edu.cornell.cs.apl.viaduct.pdg.PdgNode;
import edu.cornell.cs.apl.viaduct.protocol.ProtocolInstantiationInfo;
import edu.cornell.cs.apl.viaduct.security.Label;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public abstract class AbstractSynthesizedSingle extends AbstractSingle {
  private static class SynthesizedHostInfo {
    public String protocolId;
    public Set<HostName> hosts;

    public SynthesizedHostInfo(String protocolId, Set<HostName> hosts) {
      this.protocolId = protocolId;
      this.hosts = hosts;
    }

    @Override
    public boolean equals(Object o) {
      if (o == null) {
        return false;
      }

      if (o instanceof SynthesizedHostInfo) {
        SynthesizedHostInfo oinfo = (SynthesizedHostInfo) o;
        return this.protocolId.equals(oinfo.protocolId) && this.hosts.equals(oinfo.hosts);

      } else {
        return false;
      }
    }

    @Override
    public int hashCode() {
      return Objects.hash(protocolId, hosts);
    }
  }

  private static final Map<SynthesizedHostInfo, ProcessName> synthesizedHostMap = new HashMap<>();

  protected AbstractSynthesizedSingle(Set<HostName> hosts, Label trust) {
    super(hosts, trust);
  }

  protected AbstractSynthesizedSingle(HostName host, Label trust) {
    super(host, trust);
  }

  @Override
  public boolean hasSyntheticProcesses() {
    return true;
  }

  @Override
  public void initialize(PdgNode<ImpAstNode> node, ProtocolInstantiationInfo<ImpAstNode> info) {
    SynthesizedHostInfo synthesizedHostInfo = new SynthesizedHostInfo(getId(), this.hosts);

    if (synthesizedHostMap.containsKey(synthesizedHostInfo)) {
      this.process = synthesizedHostMap.get(synthesizedHostInfo);

    } else {
      this.process = ProcessName.create(info.getFreshName(toString()));
      synthesizedHostMap.put(synthesizedHostInfo, this.process);
      info.createProcess(this.process);
    }
  }
}
