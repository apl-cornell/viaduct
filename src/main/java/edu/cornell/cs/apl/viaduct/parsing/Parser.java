package edu.cornell.cs.apl.viaduct.parsing;

import edu.cornell.cs.apl.viaduct.syntax.surface.ProgramNode;
import java_cup.runtime.ComplexSymbolFactory;

class Parser {
  private Parser() {}

  /** Parses the source file and returns the AST. */
  static ProgramNode parse(SourceFile file) throws Exception {
    ComplexSymbolFactory symbolFactory = new ComplexSymbolFactory();
    ImpLexer scanner = new ImpLexer(file, symbolFactory);
    ImpParser parser = new ImpParser(scanner, symbolFactory);
    return (ProgramNode) parser.parse().value;
  }
}
