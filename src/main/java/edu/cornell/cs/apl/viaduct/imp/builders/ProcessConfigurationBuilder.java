package edu.cornell.cs.apl.viaduct.imp.builders;

import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.HostName;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessName;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.imp.transformers.TargetPostprocessor;
import edu.cornell.cs.apl.viaduct.util.FreshNameGenerator;
import java.util.HashMap;
import java.util.Map;

// TODO: roll into ProgramNode.Builder

/** Builds process configurations. */
public class ProcessConfigurationBuilder {
  private final Map<HostName, StmtBuilder> configBuilder;
  private final FreshNameGenerator freshNameGenerator;

  /** create statement builders for each host. */
  public ProcessConfigurationBuilder(HostTrustConfiguration config) {
    this.configBuilder = new HashMap<>();
    this.freshNameGenerator = new FreshNameGenerator();
    for (HostName h : config.hosts()) {
      this.configBuilder.put(h, new StmtBuilder());
    }
  }

  /** Retrieve the process configuration. */
  public ProgramNode build() {
    ProgramNode.Builder programBuilder = ProgramNode.builder();

    for (Map.Entry<HostName, StmtBuilder> kv : configBuilder.entrySet()) {
      ProcessName name = ProcessName.create(kv.getKey());
      BlockNode body = (BlockNode) kv.getValue().build();
      programBuilder.add(ProcessDeclarationNode.builder().setName(name).setBody(body).build());
    }

    return TargetPostprocessor.run(programBuilder.build());
  }

  /**
   * Create a new process at the given host.
   *
   * @return true if a process at the host did not already exist
   */
  public boolean createProcess(HostName h) {
    if (!this.configBuilder.containsKey(h)) {
      this.configBuilder.put(h, new StmtBuilder());
      return true;

    } else {
      return false;
    }
  }

  public StmtBuilder getBuilder(HostName h) {
    return this.configBuilder.get(h);
  }

  /** Get a fresh name. */
  public String getFreshName(String base) {
    return this.freshNameGenerator.getFreshName(base);
  }

  /** Get a fresh variable. */
  public Variable getFreshVar(String base) {
    return Variable.builder().setName(getFreshName(base)).build();
  }
}
