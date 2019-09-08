package edu.cornell.cs.apl.viaduct.imp;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import edu.cornell.cs.apl.viaduct.imp.ast.HostDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.HostName;
import edu.cornell.cs.apl.viaduct.security.Label;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.annotation.Nonnull;

/**
 * Associates each host with a label that determines maximum trust that can be placed in that host.
 */
@AutoValue
public abstract class HostTrustConfiguration implements Iterable<HostDeclarationNode> {
  public static Builder builder() {
    return new AutoValue_HostTrustConfiguration.Builder();
  }

  public abstract Builder toBuilder();

  public abstract ImmutableMap<HostName, HostDeclarationNode> getDeclarations();

  /** Return the trust associated with a host. */
  public Label getTrust(HostName host) throws NoSuchElementException {
    final HostDeclarationNode declaration = getDeclarations().get(host);
    if (declaration == null) {
      throw new NoSuchElementException(host.toString());
    }
    return declaration.getTrust();
  }

  /** Return the set of all hosts in the configuration. */
  public Set<HostName> hosts() {
    return getDeclarations().keySet();
  }

  @Override
  public @Nonnull Iterator<HostDeclarationNode> iterator() {
    return getDeclarations().values().iterator();
  }

  @Override
  public String toString() {
    final StringBuilder buffer = new StringBuilder();

    for (HostDeclarationNode declaration : this) {
      buffer.append(declaration.getName());
      buffer.append(" : ");
      buffer.append(declaration.getTrust());
      buffer.append('\n');
    }

    return buffer.toString();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    abstract ImmutableMap.Builder<HostName, HostDeclarationNode> declarationsBuilder();

    /**
     * Add the given declaration to the configuration, overwriting any existing trust declarations
     * for the same host.
     */
    public final Builder add(HostDeclarationNode declaration) {
      this.declarationsBuilder().put(declaration.getName(), declaration);
      return this;
    }

    /**
     * Add the given declarations to the configuration, overwriting existing declarations for
     * clashing hosts.
     */
    public final Builder addAll(Iterable<? extends HostDeclarationNode> declarations) {
      for (HostDeclarationNode declaration : declarations) {
        this.add(declaration);
      }
      return this;
    }

    public abstract HostTrustConfiguration build();
  }
}
