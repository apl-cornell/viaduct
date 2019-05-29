package edu.cornell.cs.apl.viaduct;

import edu.cornell.cs.apl.viaduct.imp.ImpAnnotations.ProcessAnnotation;
import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.imp.builders.StmtBuilder;
import edu.cornell.cs.apl.viaduct.imp.visitors.TargetPostprocessVisitor;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ProcessConfigBuilder {
  Map<Host, StmtBuilder> configBuilder;
  FreshNameGenerator freshNameGenerator;

  /** create statement builders for each host. */
  public ProcessConfigBuilder(Set<Host> hostConfig) {
    this.configBuilder = new HashMap<>();
    this.freshNameGenerator = new FreshNameGenerator();
    for (Host h : hostConfig) {
      this.configBuilder.put(h, new StmtBuilder());
    }
  }

  public StmtBuilder getBuilder(Host h) {
    return this.configBuilder.get(h);
  }

  /** get a fresh name. */
  public String getFreshName(String base) {
    return this.freshNameGenerator.getFreshName(base);
  }

  /** get a fresh variable. */
  public Variable getFreshVar(String base) {
    return new Variable(getFreshName(base));
  }

  /** build stmt map out of the stmt builder map. */
  public Map<Host, StmtNode> generateProcessConfig() {
    Map<Host, StmtNode> config = new HashMap<>();
    TargetPostprocessVisitor postprocessor = new TargetPostprocessVisitor();

    for (Map.Entry<Host, StmtBuilder> kv : configBuilder.entrySet()) {
      Host host = kv.getKey();
      StmtNode program = kv.getValue().build();
      StmtNode postprocessedProgram = postprocessor.postprocess(host, program);
      config.put(host, postprocessedProgram);
    }

    return config;
  }

  /** generate a single program that represents the program for all the processes. */
  public StmtNode generateSingleProgram() {
    StmtBuilder builder = new StmtBuilder();
    Map<Host, StmtNode> config = generateProcessConfig();

    for (Map.Entry<Host,StmtNode> kv : config.entrySet()) {
      ProcessAnnotation procAnnot = new ProcessAnnotation(kv.getKey());
      builder.annotation(procAnnot.toAnnotationString());

      StmtNode procStmt = kv.getValue();
      if (procStmt instanceof BlockNode) {
        BlockNode procBlock = (BlockNode)procStmt;
        for (StmtNode blockStmt : procBlock) {
          builder.statement(blockStmt);
        }
      } else {
        builder.statement(procStmt);
      }
    }

    return builder.build();
  }
}
