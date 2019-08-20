package edu.cornell.cs.apl.viaduct.imp.protocols;

import edu.cornell.cs.apl.viaduct.imp.ast.Host;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.protocol.PowersetProtocolFactory;
import edu.cornell.cs.apl.viaduct.protocol.Protocol;
import edu.cornell.cs.apl.viaduct.security.Label;

import java.util.Set;

/** contains MPC information flow constraints. */
public class MPCFactory extends PowersetProtocolFactory<ImpAstNode> {
  @Override
  protected Protocol<ImpAstNode> createInstanceFromHostInfo(Set<Host> hostSet) {
    return new MPC(hostSet);
  }

  @Override
  protected Label getProtocolLabelFromLabelSet(Set<Label> hostLabels) {
    if (hostLabels.size() > 1) {
      Label label = Label.weakest();
      for (Label hostLabel : hostLabels) {
        label = label.and(hostLabel);
      }
      return label;

    } else {
      return null;
    }
  }
}
