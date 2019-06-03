package edu.cornell.cs.apl.viaduct.imp.parser;

import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java_cup.runtime.ComplexSymbolFactory;
import java_cup.runtime.Scanner;

/** Parser for Imp source code. */
public class Parser {
  /** Read and parse the given Imp source file and return the AST. */
  public static ImpAstNode parse(File source) throws Exception {
    BufferedReader reader =
        new BufferedReader(
            new InputStreamReader(new FileInputStream(source), StandardCharsets.UTF_8));
    return parse(reader);
  }

  /** Parse the given input stream and return the AST. */
  public static ImpAstNode parse(Reader reader) throws Exception {
    ComplexSymbolFactory symbolFactory = new ComplexSymbolFactory();
    Scanner scanner = new ImpLexer(reader, symbolFactory);
    ImpParser parser = new ImpParser(scanner, symbolFactory);
    return (ImpAstNode) parser.parse().value;
  }
}
