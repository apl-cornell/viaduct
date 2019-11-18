package edu.cornell.cs.apl.viaduct.imp.protocols;

import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.HostName;
import io.vavr.Tuple2;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** non-interactive zero-knowledge proof. */
public class ZK extends AbstractSynthesizedSingle {
  private final HostName prover;
  private final HostName verifier;

  /** constructor. */
  public ZK(HostTrustConfiguration hostConfig, HostName prover, HostName verifier) {
    super(
        new HashSet<>(List.of(prover, verifier)),
        hostConfig.getTrust(prover).and(hostConfig.getTrust(verifier).integrity()));
    this.prover = prover;
    this.verifier = verifier;
  }

  @Override
  protected Object getProcessIdentity() {
    return new Tuple2<>(this.prover, this.verifier);
  }

  @Override
  public String getId() {
    return "ZK";
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
    return String.format("%s(%s,%s)", getId(), this.prover.getName(), this.verifier.getName());
  }
}
