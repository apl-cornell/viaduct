package edu.cornell.cs.apl.viaduct.imp.builders;

import edu.cornell.cs.apl.viaduct.FreshNameGenerator;
import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.Host;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessConfigurationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.imp.visitors.TargetPostprocessVisitor;
import java.util.HashMap;
import java.util.Map;

/** Builds process configurations. */
public class ProcessConfigurationBuilder {
  private final Map<Host, StmtBuilder> configBuilder;
  private final FreshNameGenerator freshNameGenerator;

  /** create statement builders for each host. */
  public ProcessConfigurationBuilder(HostTrustConfiguration config) {
    this.configBuilder = new HashMap<>();
    this.freshNameGenerator = new FreshNameGenerator();
    for (Host h : config.hosts()) {
      this.configBuilder.put(h, new StmtBuilder());
    }
  }

  /** Retrieve the process configuration. */
  public ProcessConfigurationNode build() {
    Map<Host, StmtNode> config = new HashMap<>();
    TargetPostprocessVisitor postprocessor = new TargetPostprocessVisitor();

    for (Map.Entry<Host, StmtBuilder> kv : configBuilder.entrySet()) {
      Host host = kv.getKey();
      StmtNode program = kv.getValue().build();
      StmtNode postprocessedProgram = postprocessor.postprocess(host, program);
      config.put(host, postprocessedProgram);
    }

    return new ProcessConfigurationNode(config);
  }

  public StmtBuilder getBuilder(Host h) {
    return this.configBuilder.get(h);
  }

  /** Get a fresh name. */
  public String getFreshName(String base) {
    return this.freshNameGenerator.getFreshName(base);
  }

  /** Get a fresh variable. */
  public Variable getFreshVar(String base) {
    return new Variable(getFreshName(base));
  }
}
