package edu.cornell.cs.apl.viaduct.imp.parser;

import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

/** Parser for host trust configurations. */
public class TrustConfigurationParser {
  /** Parse a host trust configuration from the given source file. */
  public static HostTrustConfiguration parse(File source) throws Exception {
    BufferedReader reader =
        new BufferedReader(
            new InputStreamReader(new FileInputStream(source), StandardCharsets.UTF_8));
    return parse(reader);
  }

  /** Parse a host trust configuration from the given input stream. */
  public static HostTrustConfiguration parse(Reader reader) throws Exception {
    ProgramNode program = Parser.parse(reader);
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
