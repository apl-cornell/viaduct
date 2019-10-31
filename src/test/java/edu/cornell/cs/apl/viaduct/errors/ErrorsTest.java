package edu.cornell.cs.apl.viaduct.errors;

import edu.cornell.cs.apl.viaduct.ErroneousExamplesProvider;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.imp.informationflow.InformationFlowChecker;
import edu.cornell.cs.apl.viaduct.imp.interpreter.Interpreter;
import edu.cornell.cs.apl.viaduct.imp.parsing.Parser;
import edu.cornell.cs.apl.viaduct.imp.parsing.SourceFile;
import edu.cornell.cs.apl.viaduct.imp.transformers.Elaborator;
import edu.cornell.cs.apl.viaduct.imp.typing.TypeChecker;
import edu.cornell.cs.apl.viaduct.util.PrintUtil;
import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

class ErrorsTest {
  // TODO: add tests for process and host name conflicts.
  // TODO: add tests for sends to and receives from unknown hosts.

  /** Parse, check, and interpret an Imp program. */
  private static void run(File file) throws IOException {
    final ProgramNode program = Parser.parse(SourceFile.from(file));
    // TODO: add name checking.
    TypeChecker.run(program);
    InformationFlowChecker.run(Elaborator.run(program));
    Interpreter.run(program);
  }

  /**
   * Check if a line is blank, i.e. contains only space and carrot ({@code '^'}) characters. Carrots
   * are considered blank since they are used to underline portions of the previous line.
   */
  private static boolean isBlank(String line) {
    return line.chars().allMatch((c) -> c == ' ' || c == '^');
  }

  @DisplayName("Bad programs should cause an error when parsed, checked, and/or interpreted.")
  @ParameterizedTest
  @ArgumentsSource(ErroneousExamplesProvider.class)
  void testRun(File file) {
    Assertions.assertThrows(CompilationError.class, () -> run(file));
  }

  @DisplayName("Error messages should end with a blank line for uniformity.")
  @ParameterizedTest
  @ArgumentsSource(ErroneousExamplesProvider.class)
  void testErrorMessagesEndWithBlankLine(File file) throws IOException {
    try {
      run(file);
    } catch (CompilationError e) {
      final String message = PrintUtil.printToString(e::print);
      final String[] lines = message.split("\\R", -1);
      final String secondFromLastLine = lines[lines.length - 2];
      final String lastLine = lines[lines.length - 1];
      Assertions.assertTrue(isBlank(secondFromLastLine));
      Assertions.assertEquals("", lastLine);
    }
  }
}
