package io.github.aplcornell.viaduct.syntax.circuit;

import io.github.aplcornell.viaduct.errors.IllegalCharacterError;

import java.io.Reader;

import java_cup.runtime.ComplexSymbolFactory;
import java_cup.runtime.ComplexSymbolFactory.Location;
import java_cup.runtime.Symbol;

import io.github.aplcornell.viaduct.parsing.*;


%%

%class CircuitLexer
%unicode
%cup

/* Turn on character counting to get access to source locations. */
%char

%{
  final SourceFile sourceFile;
  private final ComplexSymbolFactory symbolFactory;
  private int commentLevel = 0;

  /**
  * Constructs a new lexer.
  *
  * @param sourceFile input file
  * @param symbolFactory generates symbols with source location information
  */
  public CircuitLexer(SourceFile sourceFile, ComplexSymbolFactory symbolFactory) {
    this(sourceFile.createReader(), sourceFile, symbolFactory);
  }

  /** Generates a source location given left and right character offsets. */
  public SourceRange location(int left, int right) {
    final SourcePosition start = new SourcePosition(sourceFile, left);
    final SourcePosition end = new SourcePosition(sourceFile, right);
    return new SourceRange(start, end);
  }

  /**
   * Constructs a Symbol with information about source location.
   */
  private Symbol symbol(int code) {
    return symbol(code, null);
  }

  /**
   * Constructs a Symbol with information about source location.
   * Additionally stores a value.
   */
  private Symbol symbol(int code, Object value) {
    final int leftOffset = (int) yychar;
    final int rightOffset = (int) yychar + yylength();

    // TODO: make sure line and column numbers are never used, or give them nice values.
    final Location left = new Location(-1, -1, leftOffset);
    final Location right = new Location(-1, -1, rightOffset);

    return symbolFactory.newSymbol(sym.terminalNames[code], code, left, right, value);
  }
%}


/* Add these as arguments to the constructor. */
%ctorarg SourceFile sourceFile
%ctorarg ComplexSymbolFactory symbolFactory

%init{
  this.sourceFile = sourceFile;
  this.symbolFactory = symbolFactory;
%init}


/* White space is a line terminator, space, or tab. */
Whitespace     = \R | [ \t\f]

ALPHANUM    = [a-z]([A-Za-z0-9_])*
CAPALPHANUM = [A-Z]([A-Za-z0-9_])*
NUM         = ((-)?[1-9][0-9]*) | 0


/* Lexer states in addition to the default. */
%state COMMENT


%%

<YYINITIAL> {
  /* Top-level declarations */
  "host"          { return symbol(sym.HOST); }
  "fun"           { return symbol(sym.FUNCTION); }
  "circuit"       { return symbol(sym.CIRCUIT); }
  "return"        { return symbol(sym.RETURN); }

  /* Types */
  "int"           { return symbol(sym.INT); }
  "bool"          { return symbol(sym.BOOL); }
  "unit"          { return symbol(sym.UNIT); }

  /* Statements */
  "val"           { return symbol(sym.VAL); }
  ":"             { return symbol(sym.COLON); }
  "::"            { return symbol(sym.COLONCOLON); }

  "input"         { return symbol(sym.INPUT); }
  "output"        { return symbol(sym.OUTPUT); }

  /* Expressions */
  "."             { return symbol(sym.PERIOD); }

  "reduce"        { return symbol(sym.REDUCE); }

  "true"          { return symbol(sym.TRUE); }
  "false"         { return symbol(sym.FALSE); }

  {NUM}           { return symbol(sym.INT_LIT, Integer.valueOf(yytext())); }

  "!"             { return symbol(sym.NOT); }
  "&&"            { return symbol(sym.ANDAND); }
  "||"            { return symbol(sym.OROR); }

  "="             { return symbol(sym.EQ); }

  "+"             { return symbol(sym.PLUS); }
  "-"             { return symbol(sym.MINUS); }
  "*"             { return symbol(sym.TIMES); }
  "/"             { return symbol(sym.DIVIDE); }
  "min"           { return symbol(sym.MIN); }
  "max"           { return symbol(sym.MAX); }
  "mux"           { return symbol(sym.MUX); }

  "=="            { return symbol(sym.EQEQ); }
  "!="            { return symbol(sym.NEQ); }
  "<"             { return symbol(sym.LT); }
  "<="            { return symbol(sym.LEQ); }
  ">"             { return symbol(sym.GT); }
  ">="            { return symbol(sym.GEQ); }

  /* Labels */
  "<-" | "←"      { return symbol(sym.LARROW); }
  "->" | "→"      { return symbol(sym.RARROW); }

  /* Demarcate protocol annotation */
  "@"             { return symbol(sym.AT); }

/* Grouping */
  "{"             { return symbol(sym.OPEN_BRACE); }
  "}"             { return symbol(sym.CLOSE_BRACE); }
  ";"             { return symbol(sym.SEMICOLON); }
  "["             { return symbol(sym.OPEN_SQBRACE); }
  "]"             { return symbol(sym.CLOSE_SQBRACE); }
  "("             { return symbol(sym.OPEN_PAREN); }
  ")"             { return symbol(sym.CLOSE_PAREN); }
  ","             { return symbol(sym.COMMA); }

  /* Identifiers */
  {CAPALPHANUM}   { return symbol(sym.CAP_IDENT, yytext()); }
  {ALPHANUM}      { return symbol(sym.IDENT, yytext()); }

  /* Comments and Whitespace */
  "/*"            { commentLevel++; yybegin(COMMENT); }
  {Whitespace}    { /* do nothing */ }
}

<COMMENT> {
  "/*" { commentLevel++; }

  "*/" {
    commentLevel--;
    if (commentLevel == 0) {
      yybegin(YYINITIAL);
    }
  }

  .            { /* do nothing */ }

  \R           { /* do nothing */ }
}

<<EOF>>        { return symbol(sym.EOF); }

[^]            { throw new IllegalCharacterError(location((int) yychar, (int) yychar + yylength())); }
