package edu.cornell.cs.apl.viaduct.parsing;

import edu.cornell.cs.apl.viaduct.syntax.surface.ProgramNode;
import java_cup.runtime.ComplexSymbolFactory;

class Parsing {
  private Parsing() {}

  /** Parses the source file and returns the AST. */
  static ProgramNode parse(SourceFile file) throws Exception {
    ComplexSymbolFactory symbolFactory = new ComplexSymbolFactory();
    Lexer scanner = new Lexer(file, symbolFactory);
    Parser parser = new Parser(scanner, symbolFactory);
    return (ProgramNode) parser.parse().value;
  }
}
