package edu.cornell.cs.apl.viaduct.imp;

import edu.cornell.cs.apl.viaduct.imp.ast.Host;
import edu.cornell.cs.apl.viaduct.security.Label;
import io.vavr.Tuple2;
import io.vavr.collection.SortedMap;
import io.vavr.collection.TreeMap;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Associates each host with a label that determines maximum trust that can be placed in that host.
 */
public class HostTrustConfiguration implements Iterable<Tuple2<Host, Label>> {

  private final SortedMap<Host, Label> trustLevels;

  /** Construct a configuration from a list of host, statement pairs. */
  public HostTrustConfiguration(Iterable<? extends Tuple2<Host, Label>> trustConfiguration) {
    this.trustLevels = TreeMap.ofEntries(trustConfiguration);
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
  public Iterator<Tuple2<Host, Label>> iterator() {
    return this.trustLevels.iterator();
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
}
