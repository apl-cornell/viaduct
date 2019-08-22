package edu.cornell.cs.apl.viaduct.imp.parser;

import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;

/** Parser for host trust configurations. */
public class TrustConfigurationParser {
  /** Parse a host trust configuration from the given source file. */
  public static HostTrustConfiguration parse(SourceFile source) throws Exception {
    ProgramNode program = Parser.parse(source);
    return extractHostTrustConfiguration(program);
  }

  /**
   * Extract a trust configuration from a program. Asserts that the program does not define any
   * processes.
   */
  private static HostTrustConfiguration extractHostTrustConfiguration(ProgramNode program) {
    if (program.iterator().hasNext()) {
      throw new Error("Trust configuration file contains process definitions.");
    }
    return program.getHostTrustConfiguration();
  }
}
