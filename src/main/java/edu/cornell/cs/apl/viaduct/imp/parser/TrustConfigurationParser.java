package edu.cornell.cs.apl.viaduct.imp.parser;

import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import java.io.File;
import java.io.Reader;

/** Parser for host trust configurations. */
public class TrustConfigurationParser {
  /** Parse a host trust configuration from the given source file. */
  public static HostTrustConfiguration parse(File source) throws Exception {
    ProgramNode program = Parser.parse(source);
    return extractHostTrustConfiguration(program);
  }

  /**
   * Parse a host trust configuration from the given input stream.
   *
   * @param reader stream of characters to parse
   * @param inputSource description of where {@code reader} originated from (e.g. the file name)
   */
  public static HostTrustConfiguration parse(Reader reader, String inputSource) throws Exception {
    ProgramNode program = Parser.parse(reader, inputSource);
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
