package edu.cornell.cs.apl.viaduct.imp;

import edu.cornell.cs.apl.viaduct.CompilationException;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpType;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;

// TODO: this is horrible. Pull different cases into separate classes like we do with the
//   interpreter.

public class TypeCheckException extends CompilationException {
  /** AST node has unexpected type. */
  public TypeCheckException(ImpAstNode ast, ImpType expected, ImpType actual) {
    super(String.format("type check error for %s: expected %s, found %s", ast, expected, actual));
  }

  /** type mismatch between two AST nodes. */
  public TypeCheckException(ImpAstNode ast1, ImpType type1, ImpAstNode ast2, ImpType type2) {
    super(
        String.format(
            "types don't match: %s has type %s, while %s has type %s", ast1, type1, ast2, type2));
  }

  /** variable not found. */
  public TypeCheckException(Variable var) {
    super(String.format("Variable %s not found", var));
  }

  /** generic type error. */
  public TypeCheckException(String msg) {
    super(msg);
  }
}
