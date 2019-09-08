package edu.cornell.cs.apl.viaduct.imp.parser;

import java.io.Reader;

import java_cup.runtime.ComplexSymbolFactory;
import java_cup.runtime.ComplexSymbolFactory.Location;
import java_cup.runtime.Symbol;

%%

%class ImpLexer
%unicode
%cup

/* Turn on character counting to get access to source locations. */
%char


%{
  private SourceFile sourceFile;
  private ComplexSymbolFactory symbolFactory;
  private int commentLevel;

  /**
  * Construct a new lexer.
  *
  * @param source input file
  * @param sf generates symbols with source location information
  */
  public ImpLexer(SourceFile source, ComplexSymbolFactory sf) {
    this(source.getReader());
    this.sourceFile = source;
    this.symbolFactory = sf;
    commentLevel = 0;
  }

  /** Return the source file currently being scanned. */
  final SourceFile getSourceFile() {
    return this.sourceFile;
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
    final int leftOffset = yychar;
    final int rightOffset = yychar + yylength();

    // TODO: make sure line and column numbers are never used, or give them nice values.
    final Location left = new Location(-1, -1, leftOffset);
    final Location right = new Location(-1, -1, rightOffset);

    return symbolFactory.newSymbol(sym.terminalNames[code], code, left, right, value);
  }
%}


/*
 * A line terminator is a Carriage Return (\r), a Line Feed (\n), or a Carriage Return
 * followd by Line Feed.
 */
LineTerminator = \r | \n | \r\n

/* White space is a line terminator, space, tab, or line feed. */
Whitespace     = {LineTerminator} | [ \t\f]

ALPHANUM    = [a-z]([A-Za-z0-9_])*
CAPALPHANUM = [A-Z]([A-Za-z0-9_])*
NUM         = ([1-9][0-9]*) | [0-9]
ANY         = .*

%eofval{
    return symbol(sym.EOF);
%eofval}

/* Lexer states in addition to the default. */
%state COMMENT


%%

<YYINITIAL> {
  "host"          { return symbol(sym.HOST); }
  "process"       { return symbol(sym.PROCESS); }
  "protocol"      { return symbol(sym.PROTOCOL); }
  "Ideal"         { return symbol(sym.IDEAL); }

  /* types */
  "int"           { return symbol(sym.INT); }
  "bool"          { return symbol(sym.BOOL); }

  /* Statements */
  ":"             { return symbol(sym.COLON); }
  "if"            { return symbol(sym.IF); }
  "else"          { return symbol(sym.ELSE); }
  "while"         { return symbol(sym.WHILE); }
  "for"           { return symbol(sym.FOR); }
  "send"          { return symbol(sym.SEND); }
  "to"            { return symbol(sym.TO); }
  "recv"          { return symbol(sym.RECV); }
  "from"          { return symbol(sym.FROM); }
  "assert"        { return symbol(sym.ASSERT); }

  /* Expressions */
  "true"          { return symbol(sym.TRUE); }
  "false"         { return symbol(sym.FALSE); }

  {NUM}           { return symbol(sym.INT_LIT, Integer.valueOf(yytext())); }

  "!"             { return symbol(sym.NOT); }
  "&&"            { return symbol(sym.ANDAND); }
  "||"            { return symbol(sym.OROR); }

  "++"            { return symbol(sym.PLUSPLUS); }
  "+="            { return symbol(sym.PLUSEQ); }
  "--"            { return symbol(sym.MINUSMINUS); }
  "-="            { return symbol(sym.MINUSEQ); }
  "*="            { return symbol(sym.TIMESEQ); }

  "+"             { return symbol(sym.PLUS); }
  "-"             { return symbol(sym.MINUS); }
  "*"             { return symbol(sym.TIMES); }
  // "/"             { return symbol(sym.DIVIDE); }

  "=="            { return symbol(sym.EQEQ); }
  "!="            { return symbol(sym.NEQ); }
  "<"             { return symbol(sym.LT); }
  "<="            { return symbol(sym.LEQ); }
  ">"             { return symbol(sym.GT); }
  ">="            { return symbol(sym.GEQ); }
  "="             { return symbol(sym.EQ); }

  "declassify"    { return symbol(sym.DECLASSIFY); }
  "endorse"       { return symbol(sym.ENDORSE); }
  "downgrade"     { return symbol(sym.DOWNGRADE); }
  "let"           { return symbol(sym.LET); }

  /* Labels */
  "<-"            { return symbol(sym.LARROW); }
  "←"             { return symbol(sym.LARROW); }
  "->"            { return symbol(sym.RARROW); }
  "→"             { return symbol(sym.RARROW); }
  "|"             { return symbol(sym.OR); }
  "&"             { return symbol(sym.AND); }
  "∨"             { return symbol(sym.VEE); }
  "∧"             { return symbol(sym.WEDGE); }
  "⊔"             { return symbol(sym.SQCUP); }
  "⊓"             { return symbol(sym.SQCAP); }
  "⊤"             { return symbol(sym.TOP); }
  "⊥"             { return symbol(sym.BOTTOM); }

/* Grouping */
  ";"             { return symbol(sym.SEMICOLON); }
  "{"             { return symbol(sym.OPEN_BRACE); }
  "}"             { return symbol(sym.CLOSE_BRACE); }
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

  {Whitespace} { /* do nothing */ }

  .            { /* do nothing */ }
}

[^]                    { throw new Error("Illegal character <" + yytext() + ">"); }
