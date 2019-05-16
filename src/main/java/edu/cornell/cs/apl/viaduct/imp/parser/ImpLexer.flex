/* Example JFlex grammar file
 * The generated lexer class will have an API as specified here:
 * http://jflex.de/manual.html#ScannerMethods
**/

package edu.cornell.cs.apl.viaduct.imp.parser;

import java.io.Reader;

import java_cup.runtime.SymbolFactory;
import java_cup.runtime.Symbol;

%%

/* the name of your lexer class */
%class ImpLexer
%public
%cup

/* the type of the lexical token returned by the yylex function
   HINT: you should define your own token class that contains more information!
*/
%type Symbol

/* declare variables and methods */
%{
  /* To create a new java_cup.runtime.Symbol with information about
      the current token, the token will have no value in this
      case. */
  private SymbolFactory symbolFactory;
  private int commentLevel;

  public ImpLexer(Reader r, SymbolFactory sf) {
    this(r);
    commentLevel = 0;
    symbolFactory = sf;
  }

  private Symbol symbol(int type) {
      return symbolFactory.newSymbol(sym.terminalNames[type], type);
  }

  /* Also creates a new java_cup.runtime.Symbol with information
      about the current token, but this object has a value. */
  private Symbol symbol(int type, Object value) {
      return symbolFactory.newSymbol(sym.terminalNames[type], type, value);
  }
%}

/* switch line counting on */
%line

/* declare a new lexical state */
%state COMMENT

/* macro */
/* A line terminator is a \r (carriage return), \n (line feed), or
   \r\n. */
LineTerminator = \r|\n|\r\n
/* White space is a line terminator, space, tab, or line feed. */
Whitespace     = {LineTerminator} | [ \t\f]

ALPHANUM=[A-Za-z_]([A-Za-z0-9_])*
CAPALPHANUM=[A-Z_]([A-Z0-9_])*
NUM=[0-9]
ANY=.*

%%

<YYINITIAL> {
  "+"             { return symbol(sym.PLUS); }
  // "-"             { return symbol(sym.MINUS); }
  // "*"             { return symbol(sym.TIMES); }
  // "/"             { return symbol(sym.DIVIDE); }
  "<-"            { return symbol(sym.LARROW); }
  "->"            { return symbol(sym.RARROW); }
  "<="            { return symbol(sym.LEQ); }
  ">="            { return symbol(sym.GEQ); }
  "<"             { return symbol(sym.LT); }
  ">"             { return symbol(sym.GT); }
  ":="            { return symbol(sym.ASSIGN); }
  "=="            { return symbol(sym.EQ); }
  "!="            { return symbol(sym.NEQ); }
  "&&"            { return symbol(sym.ANDAND); }
  "&"             { return symbol(sym.AND); }
  "||"            { return symbol(sym.OROR); }
  "|"             { return symbol(sym.OR); }
  "!"             { return symbol(sym.NOT); }
  "{"             { return symbol(sym.OPEN_BRACE); }
  "}"             { return symbol(sym.CLOSE_BRACE); }
  "("             { return symbol(sym.OPEN_PAREN); }
  ")"             { return symbol(sym.CLOSE_PAREN); }
  ";"             { return symbol(sym.SEMICOLON); }
  ":"             { return symbol(sym.COLON); }
  ","             { return symbol(sym.COMMA); }
  "⊤"            { return symbol(sym.TOP); }
  "⊥"            { return symbol(sym.BOTTOM); }
  "if"            { return symbol(sym.IF); }
  "else"          { return symbol(sym.ELSE); }
  "send"          { return symbol(sym.SEND); }
  "recv"          { return symbol(sym.RECV); }
  "to"            { return symbol(sym.TO); }
  "from"          { return symbol(sym.FROM); }
  "true"          { return symbol(sym.TRUE); }
  "false"         { return symbol(sym.FALSE); }
  "skip"          { return symbol(sym.SKIP); }
  "declassify"    { return symbol(sym.DECLASSIFY); }
  "endorse"       { return symbol(sym.ENDORSE); }
  "/*"            { commentLevel++; yybegin(COMMENT); }

  {NUM} { return symbol(sym.INT_LIT, Integer.valueOf(yytext())); }

  {CAPALPHANUM}  { return symbol(sym.CAP_IDENT, yytext()); }

  {ALPHANUM}  { return symbol(sym.IDENT, yytext()); }

  {Whitespace} { /* do nothing */ }
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

[^]                    { throw new Error("Illegal character <"+yytext()+">"); }
