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

public abstract class AbstractSynthesizedSingle extends AbstractSingle {
  private static class SynthesizedHostInfo {
    public final String protocolId;
    public final Object processIdentity;

    public SynthesizedHostInfo(String protocolId, Object processIdentity) {
      this.protocolId = protocolId;
      this.processIdentity = processIdentity;
    }

    @Override
    public boolean equals(Object o) {
      if (o == null) {
        return false;
      }

      if (o instanceof SynthesizedHostInfo) {
        SynthesizedHostInfo oinfo = (SynthesizedHostInfo) o;
        return this.protocolId.equals(oinfo.protocolId)
            && this.processIdentity.equals(oinfo.processIdentity);

      } else {
        return false;
      }
    }

    @Override
    public int hashCode() {
      return Objects.hash(protocolId, this.processIdentity);
    }
  }

  private static final Map<SynthesizedHostInfo, ProcessName> synthesizedHostMap = new HashMap<>();

  protected AbstractSynthesizedSingle(java.util.Set<HostName> hosts, Label trust) {
    super(hosts, trust);
  }

  protected AbstractSynthesizedSingle(HostName host, Label trust) {
    super(host, trust);
  }

  protected abstract Object getProcessIdentity();

  @Override
  public boolean hasSyntheticProcesses() {
    return true;
  }

  @Override
  public void initialize(PdgNode<ImpAstNode> node, ProtocolInstantiationInfo<ImpAstNode> info) {
    SynthesizedHostInfo synthesizedHostInfo =
        new SynthesizedHostInfo(getId(), getProcessIdentity());

    if (synthesizedHostMap.containsKey(synthesizedHostInfo)) {
      this.process = synthesizedHostMap.get(synthesizedHostInfo);

    } else {
      this.process = ProcessName.createFreshName();
      synthesizedHostMap.put(synthesizedHostInfo, this.process);
      info.createProcess(this.process, this);
    }
  }
}
