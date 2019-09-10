package edu.cornell.cs.apl.viaduct.errors;

import edu.cornell.cs.apl.viaduct.ErroneousExamplesProvider;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.imp.interpreter.Interpreter;
import edu.cornell.cs.apl.viaduct.imp.parsing.Parser;
import edu.cornell.cs.apl.viaduct.imp.parsing.SourceFile;
import edu.cornell.cs.apl.viaduct.imp.typing.TypeChecker;
import java.io.File;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

class ErrorsTest {
  @DisplayName("Bad programs should cause an error when checked and/or interpreted.")
  @ParameterizedTest
  @ArgumentsSource(ErroneousExamplesProvider.class)
  void testRun(File file) {
    Assertions.assertThrows(
        CompilationError.class,
        () -> {
          final ProgramNode program = Parser.parse(SourceFile.from(file));
          TypeChecker.run(program);
          Interpreter.run(program);
        });
  }
}
