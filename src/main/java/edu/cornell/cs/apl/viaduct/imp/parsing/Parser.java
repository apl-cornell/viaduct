package edu.cornell.cs.apl.viaduct.imp.parsing;

import edu.cornell.cs.apl.viaduct.errors.ParsingError;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import java.io.IOException;
import java.io.StringReader;
import java_cup.runtime.ComplexSymbolFactory;
import java_cup.runtime.Scanner;

/** Parser for Imp source code. */
public final class Parser {
  /** Parse the given string and return the AST. */
  public static ProgramNode parse(String source) throws IOException, ParsingError {
    return parse(SourceFile.from("<string>", new StringReader(source)));
  }

  /** Read and parse the given Imp source file and return the AST. */
  public static ProgramNode parse(SourceFile sourceFile) throws ParsingError {
    try {
      ComplexSymbolFactory symbolFactory = new ComplexSymbolFactory();
      Scanner scanner = new ImpLexer(sourceFile, symbolFactory);
      ImpParser parser = new ImpParser(scanner, symbolFactory);
      return (ProgramNode) parser.parse().value;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
