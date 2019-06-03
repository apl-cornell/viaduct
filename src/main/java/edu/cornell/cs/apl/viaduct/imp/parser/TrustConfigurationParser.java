package edu.cornell.cs.apl.viaduct.imp.parser;

import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.DeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Host;
import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;
import edu.cornell.cs.apl.viaduct.security.Label;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java_cup.runtime.ComplexSymbolFactory;
import java_cup.runtime.Scanner;

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
    ComplexSymbolFactory symbolFactory = new ComplexSymbolFactory();
    Scanner scanner = new ImpLexer(reader, symbolFactory);
    ImpParser parser = new ImpParser(scanner, symbolFactory);
    // TODO: don't repurpose DeclarationNode.
    BlockNode declarations = (BlockNode) parser.parse().value;

    List<Tuple2<Host, Label>> config = new LinkedList<>();
    for (StmtNode stmt : declarations) {
      if (stmt instanceof DeclarationNode) {
        DeclarationNode hostDecl = (DeclarationNode) stmt;
        String hostName = hostDecl.getVariable().toString();
        Label hostLabel = hostDecl.getLabel();
        config.add(Tuple.of(new Host(hostName), hostLabel));
      } else {
        throw new Exception("Invalid host configuration");
      }
    }

    return new HostTrustConfiguration(config);
  }
}
