package edu.cornell.cs.apl.viaduct.errors;

import edu.cornell.cs.apl.viaduct.ErroneousExamplesProvider;
import edu.cornell.cs.apl.viaduct.ImpAstParser;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.imp.interpreter.Interpreter;
import edu.cornell.cs.apl.viaduct.imp.typing.TypeChecker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.provider.ArgumentsSource;

class ErrorsTest {
  @DisplayName("Bad programs should cause an error when checked and/or interpreted.")
  @ParameterizedTest
  @ArgumentsSource(ErroneousExamplesProvider.class)
  void testRun(@ConvertWith(ImpAstParser.class) ProgramNode program) {
    Assertions.assertThrows(
        CompilationError.class,
        () -> {
          TypeChecker.run(program);
          Interpreter.run(program);
        });
  }
}
