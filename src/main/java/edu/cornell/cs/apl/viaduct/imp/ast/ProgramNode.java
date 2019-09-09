package edu.cornell.cs.apl.viaduct.imp.ast;

import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import edu.cornell.cs.apl.viaduct.imp.visitors.ImpAstVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.ProgramVisitor;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.annotation.Nonnull;

/** A program is a sequence of top level declarations. */
@AutoValue
public abstract class ProgramNode extends ImpAstNode implements Iterable<TopLevelDeclarationNode> {
  public static Builder builder() {
    return new $AutoValue_ProgramNode.Builder();
  }

  public abstract Builder toBuilder();

  public abstract ImmutableList<TopLevelDeclarationNode> getDeclarations();

  /** Return a mapping from process names to process declarations. Useful for efficient access. */
  @Memoized
  public ImmutableMap<ProcessName, ProcessDeclarationNode> processes() {
    final Map<ProcessName, ProcessDeclarationNode> processes = new HashMap<>();
    for (TopLevelDeclarationNode declaration : getDeclarations()) {
      if (declaration instanceof ProcessDeclarationNode) {
        final ProcessDeclarationNode processDeclaration = (ProcessDeclarationNode) declaration;
        assert !processes.containsKey(processDeclaration.getName());
        processes.put(processDeclaration.getName(), processDeclaration);
      }
    }
    return ImmutableMap.copyOf(processes);
  }

  /** Return a mapping from host names to host declarations. Useful for efficient access. */
  @Memoized
  public ImmutableMap<HostName, HostDeclarationNode> hosts() {
    final Map<HostName, HostDeclarationNode> hosts = new HashMap<>();
    for (TopLevelDeclarationNode declaration : getDeclarations()) {
      if (declaration instanceof HostDeclarationNode) {
        final HostDeclarationNode hostDeclaration = (HostDeclarationNode) declaration;
        assert !hosts.containsKey(hostDeclaration.getName());
        hosts.put(hostDeclaration.getName(), hostDeclaration);
      }
    }
    return ImmutableMap.copyOf(hosts);
  }

  @Override
  public final @Nonnull Iterator<TopLevelDeclarationNode> iterator() {
    return getDeclarations().iterator();
  }

  public <R> R accept(ProgramVisitor<R> v) {
    return v.visit(this);
  }

  @Override
  public final <R> R accept(ImpAstVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @AutoValue.Builder
  public abstract static class Builder extends ImpAstNode.Builder<Builder> {
    public abstract Builder setDeclarations(
        Iterable<? extends TopLevelDeclarationNode> declarations);

    public abstract ImmutableList.Builder<TopLevelDeclarationNode> declarationsBuilder();

    public final Builder add(TopLevelDeclarationNode declaration) {
      this.declarationsBuilder().add(declaration);
      return this;
    }

    public final Builder addAll(Iterable<? extends TopLevelDeclarationNode> declarations) {
      this.declarationsBuilder().addAll(declarations);
      return this;
    }

    public abstract ProgramNode build();
  }
}
