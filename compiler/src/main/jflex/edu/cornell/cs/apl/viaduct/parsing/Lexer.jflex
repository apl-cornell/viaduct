package edu.cornell.cs.apl.viaduct.parsing;

import edu.cornell.cs.apl.viaduct.errors.IllegalCharacterError;

import java.io.Reader;

import java_cup.runtime.ComplexSymbolFactory;
import java_cup.runtime.ComplexSymbolFactory.Location;
import java_cup.runtime.Symbol;

%%

%class Lexer
%unicode
%cup

/* Turn on character counting to get access to source locations. */
%char

%{
  private final SourceFile sourceFile;
  private final ComplexSymbolFactory symbolFactory;
  private int commentLevel = 0;

  /**
  * Construct a new lexer.
  *
  * @param sourceFile input file
  * @param symbolFactory generates symbols with source location information
  */
  public Lexer(SourceFile sourceFile, ComplexSymbolFactory symbolFactory) {
    this(sourceFile.createReader(), sourceFile, symbolFactory);
  }

  /** Generate a source location given left and right character offsets. */
  public SourceRange location(int left, int right) {
    final SourcePosition start = new SourcePosition(sourceFile, left);
    final SourcePosition end = new SourcePosition(sourceFile, right);
    return new SourceRange(start, end);
  }

  /**
   * Construct a Symbol with information about source location.
   */
  private Symbol symbol(int code) {
    return symbol(code, null);
  }

  /**
   * Construct a Symbol with information about source location.
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
TMPALPHANUM = \044([A-Za-z0-9_])*
NUM         = ((-)?[1-9][0-9]*) | 0


/* Lexer states in addition to the default. */
%state COMMENT


%%

<YYINITIAL> {
  /* top-level declarations */
  "host"          { return symbol(sym.HOST); }
  "process"       { return symbol(sym.PROCESS); }
  "fun"           { return symbol(sym.FUNCTION); }
  "Host"          { return symbol(sym.CAP_HOST); }
  "out"           { return symbol(sym.OUT); }

  /* Types */
  "int"           { return symbol(sym.INT); }
  "bool"          { return symbol(sym.BOOL); }
  "unit"          { return symbol(sym.UNIT); }
  "Array"         { return symbol(sym.ARRAY); }

  /* Statements */
  "let"           { return symbol(sym.LET); }
  "val"           { return symbol(sym.VAL); }
  "var"           { return symbol(sym.VAR); }
  ":"             { return symbol(sym.COLON); }

  "if"            { return symbol(sym.IF); }
  "else"          { return symbol(sym.ELSE); }
  "while"         { return symbol(sym.WHILE); }
  "for"           { return symbol(sym.FOR); }
  "loop"          { return symbol(sym.LOOP); }
  "break"         { return symbol(sym.BREAK); }

  "input"         { return symbol(sym.INPUT); }
  "receive"       { return symbol(sym.RECEIVE); }
  "from"          { return symbol(sym.FROM); }
  "output"        { return symbol(sym.OUTPUT); }
  "send"          { return symbol(sym.SEND); }
  "to"            { return symbol(sym.TO); }

  "assert"        { return symbol(sym.ASSERT); }

  /* Expressions */
  "true"          { return symbol(sym.TRUE); }
  "false"         { return symbol(sym.FALSE); }

  {NUM}           { return symbol(sym.INT_LIT, Integer.valueOf(yytext())); }

  "!"             { return symbol(sym.NOT); }
  "&&"            { return symbol(sym.ANDAND); }
  "||"            { return symbol(sym.OROR); }

  "="             { return symbol(sym.EQ); }
  "+="            { return symbol(sym.PLUSEQ); }
  "-="            { return symbol(sym.MINUSEQ); }
  "*="            { return symbol(sym.TIMESEQ); }
  "/="            { return symbol(sym.DIVIDEEQ); }

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

  "declassify"    { return symbol(sym.DECLASSIFY); }
  "endorse"       { return symbol(sym.ENDORSE); }
  "downgrade"     { return symbol(sym.DOWNGRADE); }

  /* Labels */
  "⊤"             { return symbol(sym.TOP); }
  "⊥"             { return symbol(sym.BOTTOM); }
  "|" | "∨"       { return symbol(sym.OR); }
  "&" | "∧"       { return symbol(sym.AND); }
  "⊔"             { return symbol(sym.JOIN); }
  "⊓"             { return symbol(sym.MEET); }
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
  {TMPALPHANUM}   { return symbol(sym.TMP_IDENT, yytext()); }

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
