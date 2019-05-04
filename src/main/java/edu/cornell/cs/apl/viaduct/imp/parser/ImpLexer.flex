/* Example JFlex grammar file
 * The generated lexer class will have an API as specified here:
 * http://jflex.de/manual.html#ScannerMethods
**/

package edu.cornell.cs.apl.viaduct.imp.parser;

%%

/* the name of your lexer class */
%class ImpLexer

/* the type of the lexical token returned by the yylex function
   HINT: you should define your own token class that contains more information!
*/
%type Object

/* declare variables */
%{
  private int bar = 0;
  private int SYM_ALPHA = 0;
  private int SYM_NUM   = 1;
  private int SYM_COLON = 2;
%} 

/* switch line counting on */
%line

/* declare a new lexical state */
%state FOO

/* macro */
ALPHA=[A-Za-z]
NUM=[0-9]

%% 

/* lexical rules */
{ALPHA}  {
  return SYM_ALPHA;
}
