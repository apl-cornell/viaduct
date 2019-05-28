package edu.cornell.cs.apl.viaduct;

import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.imp.builders.StmtBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ProcessConfigBuilder {
  static final String TMP_VAR = "tmp";

  Map<Host, StmtBuilder> configBuilder;
  int varNum;

  /** create statement builders for each host. */
  public ProcessConfigBuilder(Set<Host> hostConfig) {
    this.varNum = 0;
    this.configBuilder = new HashMap<>();
    for (Host h : hostConfig) {
      this.configBuilder.put(h, new StmtBuilder());
    }
  }

  public StmtBuilder getBuilder(Host h) {
    return this.configBuilder.get(h);
  }

  /** get a fresh variable. */
  public Variable getFreshVar() {
    varNum++;
    String varName = String.format("%s%d", TMP_VAR, varNum);
    return new Variable(varName);
  }

  /** build stmt map out of the stmt builder map. */
  public Map<Host, StmtNode> buildProcessConfig() {
    Map<Host, StmtNode> config = new HashMap<>();
    for (Map.Entry<Host, StmtBuilder> kv : configBuilder.entrySet()) {
      config.put(kv.getKey(), kv.getValue().build());
    }

    return config;
  }

  /** generate a single program that represents the program for all the processes. */
  /*
  public StmtNode generateSingleProgram() {
    StmtBuilder builder = new StmtBuilder();

    for (Map.Entry<Host,StmtNode> kv : this.config.entrySet()) {
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
  */
}
