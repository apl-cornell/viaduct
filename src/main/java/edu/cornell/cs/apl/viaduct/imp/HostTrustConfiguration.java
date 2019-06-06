package edu.cornell.cs.apl.viaduct.imp;

import edu.cornell.cs.apl.viaduct.imp.ast.Host;
import edu.cornell.cs.apl.viaduct.security.Label;
import io.vavr.Tuple2;
import io.vavr.collection.SortedMap;
import io.vavr.collection.TreeMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;

/**
 * Associates each host with a label that determines maximum trust that can be placed in that host.
 */
public final class HostTrustConfiguration implements Iterable<Tuple2<Host, Label>> {
  private final SortedMap<Host, Label> trustLevels;

  /** Construct a configuration from a list of host, statement pairs. */
  private HostTrustConfiguration(SortedMap<Host, Label> trustLevels) {
    this.trustLevels = trustLevels;
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Return the trust associated with a host. */
  public Label getTrust(Host host) throws NoSuchElementException {
    return trustLevels.get(host).getOrElseThrow(() -> new NoSuchElementException(host.toString()));
  }

  /** Return a list of all hosts in the configuration. */
  public Iterable<Host> hosts() {
    return this.trustLevels.keySet();
  }

  public Set<Host> hostSet() {
    return this.trustLevels.keySet().toJavaSet();
  }

  /** Return the number of hosts in the configuration. */
  public int size() {
    return trustLevels.size();
  }

  @Override
  public @Nonnull Iterator<Tuple2<Host, Label>> iterator() {
    return this.trustLevels.iterator();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof HostTrustConfiguration)) {
      return false;
    }

    final HostTrustConfiguration that = (HostTrustConfiguration) o;
    return Objects.equals(this.trustLevels, that.trustLevels);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.trustLevels);
  }

  @Override
  public String toString() {
    final StringBuilder buffer = new StringBuilder();

    boolean first = true;
    for (Tuple2<Host, Label> entry : trustLevels) {
      if (!first) {
        buffer.append("\n");
      }

      buffer.append(entry._1());
      buffer.append(" : ");
      buffer.append(entry._2());

      first = false;
    }

    return buffer.toString();
  }

  public static final class Builder {
    private final Map<Host, Label> hosts = new HashMap<>();

    private Builder() {}

    /** Return the built trust configuration. */
    public HostTrustConfiguration build() {
      return new HostTrustConfiguration(TreeMap.ofAll(hosts));
    }

    /**
     * Add a host to the configuration with the given trust level.
     *
     * @param host host to declare a trust level for
     * @param trust trust label to associate with {@code host}
     * @throws DuplicateHostDeclarationException if {@code host} already had an associated trust
     *     level
     */
    public Builder addHost(Host host, Label trust) throws DuplicateHostDeclarationException {
      Objects.requireNonNull(host);
      Objects.requireNonNull(trust);

      if (hosts.containsKey(host)) {
        throw new DuplicateHostDeclarationException(host);
      }
      hosts.put(host, trust);

      return this;
    }

    /**
     * Add all declarations in the given trust configuration. Overwrites existing declarations with
     * the new ones if there are clashes instead of throwing an exception.
     */
    public Builder addAll(HostTrustConfiguration declarations) {
      Objects.requireNonNull(declarations);

      hosts.putAll(declarations.trustLevels.toJavaMap());

      return this;
    }
  }
}
