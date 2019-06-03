package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.AstVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.ProcessConfigurationVisitor;
import io.vavr.Tuple2;
import io.vavr.collection.SortedMap;
import io.vavr.collection.TreeMap;
import java.util.Iterator;
import javax.annotation.Nonnull;

/** Associates hosts with the statement they should run. */
public final class ProcessConfigurationNode extends ImpAstNode
    implements Iterable<Tuple2<Host, StmtNode>> {
  private final SortedMap<Host, StmtNode> processes;

  /** Construct a configuration from a list of host, statement pairs. */
  public ProcessConfigurationNode(Iterable<? extends Tuple2<Host, StmtNode>> processes) {
    this.processes = TreeMap.ofEntries(processes);
  }

  /** Construct a configuration from a host to statement mapping. */
  public ProcessConfigurationNode(java.util.Map<Host, StmtNode> processes) {
    this.processes = TreeMap.ofAll(processes);
  }

  /** Return a list of all hosts in the configuration. */
  public Iterable<Host> hosts() {
    return this.processes.keySet();
  }

  @Override
  public @Nonnull Iterator<Tuple2<Host, StmtNode>> iterator() {
    return this.processes.iterator();
  }

  public <R> R accept(ProcessConfigurationVisitor<R> v) {
    return v.visit(this);
  }

  @Override
  public <R> R accept(AstVisitor<R> v) {
    return this.accept((ProcessConfigurationVisitor<R>) v);
  }
}
