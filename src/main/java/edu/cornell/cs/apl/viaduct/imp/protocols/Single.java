package edu.cornell.cs.apl.viaduct.imp.protocols;

import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.HostName;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.pdg.PdgNode;
import edu.cornell.cs.apl.viaduct.protocol.ProtocolInstantiationInfo;

/** protocol where data and computation is stored/performed
 * in cleatext in a single host. */
public class Single extends AbstractSingle {
  /** constructor. */
  public Single(HostTrustConfiguration hostConfig, HostName h) {
    super(h, hostConfig.getTrust(h));
    this.actualHost = h;
  }

  @Override
  public String getId() {
    return "Single";
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
      return this.actualHost.equals(osingle.actualHost);

    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return this.actualHost.hashCode();
  }
}
