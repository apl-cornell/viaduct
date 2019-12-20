package edu.cornell.cs.apl.viaduct.imp.protocols;

import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.HostName;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessName;
import edu.cornell.cs.apl.viaduct.pdg.PdgNode;
import edu.cornell.cs.apl.viaduct.protocol.ProtocolInstantiationInfo;

/** protocol where data and computation is stored/performed in cleatext in a single host. */
public class Single extends AbstractSingle {
  /** constructor. */
  public Single(HostTrustConfiguration hostConfig, HostName h) {
    super(h, hostConfig.getTrust(h));
    this.process = ProcessName.create(h);
  }

  public HostName getHost() {
    return (HostName) this.hosts.toArray()[0];
  }

  @Override
  public String getId() {
    return "Single";
  }

  @Override
  public boolean hasSyntheticProcesses() {
    return false;
  }

  @Override
  public void initialize(PdgNode<ImpAstNode> node, ProtocolInstantiationInfo<ImpAstNode> info) {
    return;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }

    if (o instanceof Single) {
      Single osingle = (Single) o;
      return this.process.equals(osingle.process);

    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return this.process.hashCode();
  }
}
