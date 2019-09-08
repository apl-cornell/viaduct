package edu.cornell.cs.apl.viaduct.imp.visitors;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.cornell.cs.apl.viaduct.ExamplesProvider;
import edu.cornell.cs.apl.viaduct.ImpAstParser;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.imp.transformers.AnfConverter;
import edu.cornell.cs.apl.viaduct.imp.transformers.Elaborator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.provider.ArgumentsSource;

class AnfIdempotenceTest {
  @ParameterizedTest
  @ArgumentsSource(ExamplesProvider.class)
  void testRun(@ConvertWith(ImpAstParser.class) ProgramNode ast) {
    // Ensure that A-normal form translation is idempotent.

    final ProgramNode program = Elaborator.run(ast);
    final ProgramNode anfProgram = AnfConverter.run(program);
    final ProgramNode anfAnfProgram = AnfConverter.run(anfProgram);

    assertEquals(anfProgram, anfAnfProgram);
  }
}
