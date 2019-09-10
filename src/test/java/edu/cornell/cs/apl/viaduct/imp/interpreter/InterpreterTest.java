package edu.cornell.cs.apl.viaduct.imp.interpreter;

import edu.cornell.cs.apl.viaduct.ExamplesProvider;
import edu.cornell.cs.apl.viaduct.ImpAstParser;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.provider.ArgumentsSource;

class InterpreterTest {
  @ParameterizedTest
  @ArgumentsSource(ExamplesProvider.class)
  void testInterpreter(@ConvertWith(ImpAstParser.class) ProgramNode program) {
    // Interpret example programs to completion.
    // We do no check any outputs; programs should include assert statements.
    Interpreter.run(program);
  }
}
