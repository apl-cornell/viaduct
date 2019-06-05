package edu.cornell.cs.apl.viaduct.imp.parser;

import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java_cup.runtime.ComplexSymbolFactory;
import java_cup.runtime.Scanner;

/** Parser for Imp source code. */
public class Parser {
  /** Read and parse the given Imp source file and return the AST. */
  public static ProgramNode parse(File source) throws Exception {
    BufferedReader reader =
        new BufferedReader(
            new InputStreamReader(new FileInputStream(source), StandardCharsets.UTF_8));
    return parse(reader);
  }

  /** Parse the given string and return the AST. */
  public static ProgramNode parse(String source) throws Exception {
    return parse(new StringReader(source));
  }

  /** Parse the given input stream and return the AST. */
  public static ProgramNode parse(Reader reader) throws Exception {
    ComplexSymbolFactory symbolFactory = new ComplexSymbolFactory();
    Scanner scanner = new ImpLexer(reader, symbolFactory);
    ImpParser parser = new ImpParser(scanner, symbolFactory);
    return (ProgramNode) parser.parse().value;
  }
}
