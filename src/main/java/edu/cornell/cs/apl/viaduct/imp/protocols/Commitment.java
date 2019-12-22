package edu.cornell.cs.apl.viaduct.imp.protocols;

import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.HostName;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import io.vavr.Tuple3;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** non-interactive zero-knowledge proof. */
public class Commitment extends AbstractSynthesizedSingle {
  private final HostName sender;
  private final HostName receiver;
  private final Variable variable;

  /** constructor. */
  public Commitment(
      HostTrustConfiguration hostConfig, HostName sender, HostName receiver, Variable var) {
    super(
        new HashSet<>(List.of(sender, receiver)),
        hostConfig.getTrust(sender).and(hostConfig.getTrust(receiver).integrity()));
    this.sender = sender;
    this.receiver = receiver;
    this.variable = var;
  }

  @Override
  protected Object getProcessIdentity() {
    return new Tuple3<>(this.sender, this.receiver, this.variable);
  }

  @Override
  public String getId() {
    return "Commitment";
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }

    if (o instanceof Commitment) {
      Commitment ocommit = (Commitment) o;
      boolean seq = this.sender.equals(ocommit.sender);
      boolean req = this.receiver.equals(ocommit.receiver);
      return seq && req;

    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.sender, this.receiver);
  }

  @Override
  public String toString() {
    return String.format(
        "%s(%s,%s) for %s", getId(), this.sender, this.receiver, this.variable.getBinding());
  }
}
