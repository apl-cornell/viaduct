package edu.cornell.cs.apl.viaduct.imp.visitors;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.cornell.cs.apl.viaduct.ExamplesProvider;
import edu.cornell.cs.apl.viaduct.ImpAstParser;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.provider.ArgumentsSource;

class AnfIdempotenceTest {
  @ParameterizedTest
  @ArgumentsSource(ExamplesProvider.class)
  void testRun(@ConvertWith(ImpAstParser.class) ProgramNode ast) {
    // Ensure that A-normal form translation is idempotent.

    final ProgramNode program = new ElaborationVisitor().run(ast);
    final AnfVisitor anfRewriter = new AnfVisitor();

    ProgramNode anfConfig1 = anfRewriter.run(program);
    ProgramNode anfConfig2 = anfRewriter.run(anfConfig1);

    assertEquals(anfConfig1, anfConfig2);
  }
}
