package edu.cornell.cs.apl.viaduct.imp.parser;

import java.io.Reader;

import java_cup.runtime.ComplexSymbolFactory;
import java_cup.runtime.ComplexSymbolFactory.Location;
import java_cup.runtime.Symbol;

%%

%class ImpLexer
%unicode
%cup

/* Type of tokens returned by the yylex function. */
%type Symbol

/* Turn on line and column counting to get access to source locations. */
%line
%column


%{
  private ComplexSymbolFactory symbolFactory;
  private int commentLevel;

  public ImpLexer(Reader r, ComplexSymbolFactory sf) {
    this(r);
    commentLevel = 0;
    symbolFactory = sf;
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
    Location left = new Location(yyline + 1, yycolumn + 1 - yylength());
    Location right = new Location(yyline + 1, yycolumn + 1);
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

ALPHANUM    = [A-Za-z_]([A-Za-z0-9_])*
CAPALPHANUM = [A-Z_]([A-Z0-9_])*
NUM         = [0-9]
ANY         = .*

%eofval{
    return symbol(sym.EOF);
%eofval}

/* Lexer states in addition to the default. */
%state COMMENT


%%

<YYINITIAL> {
  "host"          { return symbol(sym.HOST); }

  /* Statements */
  ":"             { return symbol(sym.COLON); }
  ":="            { return symbol(sym.ASSIGN); }
  "if"            { return symbol(sym.IF); }
  "else"          { return symbol(sym.ELSE); }
  "send"          { return symbol(sym.SEND); }
  "to"            { return symbol(sym.TO); }
  "recv"          { return symbol(sym.RECV); }
  "from"          { return symbol(sym.FROM); }
  "assert"          { return symbol(sym.ASSERT); }

  /* Expressions */
  "true"          { return symbol(sym.TRUE); }
  "false"         { return symbol(sym.FALSE); }

  {NUM}           { return symbol(sym.INT_LIT, Integer.valueOf(yytext())); }

  "!"             { return symbol(sym.NOT); }
  "&&"            { return symbol(sym.ANDAND); }
  "||"            { return symbol(sym.OROR); }

  "+"             { return symbol(sym.PLUS); }
  // "-"             { return symbol(sym.MINUS); }
  // "*"             { return symbol(sym.TIMES); }
  // "/"             { return symbol(sym.DIVIDE); }
  "=="            { return symbol(sym.EQ); }
  "!="            { return symbol(sym.NEQ); }
  "<"             { return symbol(sym.LT); }
  "<="            { return symbol(sym.LEQ); }
  ">"             { return symbol(sym.GT); }
  ">="            { return symbol(sym.GEQ); }

  "declassify"    { return symbol(sym.DECLASSIFY); }
  "endorse"       { return symbol(sym.ENDORSE); }
  "downgrade"     { return symbol(sym.DOWNGRADE); }

  /* Labels */
  "<-"            { return symbol(sym.LARROW); }
  "->"            { return symbol(sym.RARROW); }
  "⊤"             { return symbol(sym.TOP); }
  "|"             { return symbol(sym.OR); }
  "&"             { return symbol(sym.AND); }
  "⊥"             { return symbol(sym.BOTTOM); }

  /* Grouping */
  ";"             { return symbol(sym.SEMICOLON); }
  "{"             { return symbol(sym.OPEN_BRACE); }
  "}"             { return symbol(sym.CLOSE_BRACE); }

  "("             { return symbol(sym.OPEN_PAREN); }
  ")"             { return symbol(sym.CLOSE_PAREN); }
  ","             { return symbol(sym.COMMA); }

  /* Annotations */
  "@DebugValue"   { return symbol(sym.DEBUG_VALUE); }

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
