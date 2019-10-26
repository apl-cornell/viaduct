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
  private final Map<ProcessName, StmtBuilder> configBuilder;
  private final FreshNameGenerator freshNameGenerator;

  /** create statement builders for each host. */
  public ProcessConfigurationBuilder(HostTrustConfiguration config) {
    this.configBuilder = new HashMap<>();
    this.freshNameGenerator = new FreshNameGenerator();
    for (HostName host : config.hosts()) {
      this.configBuilder.put(ProcessName.create(host), new StmtBuilder());
    }
  }

  /** Retrieve the process configuration. */
  public ProgramNode build() {
    ProgramNode.Builder programBuilder = ProgramNode.builder();

    for (Map.Entry<ProcessName, StmtBuilder> kv : configBuilder.entrySet()) {
      ProcessName name = kv.getKey();
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
  public boolean createProcess(ProcessName process) {
    if (!this.configBuilder.containsKey(process)) {
      this.configBuilder.put(process, new StmtBuilder());
      return true;

    } else {
      return false;
    }
  }

  public StmtBuilder getBuilder(ProcessName process) {
    return this.configBuilder.get(process);
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
