package edu.cornell.cs.apl.viaduct.imp.informationflow;

import edu.cornell.cs.apl.viaduct.ExamplesProvider;
import edu.cornell.cs.apl.viaduct.ImpAstParser;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.imp.transformers.Elaborator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.provider.ArgumentsSource;

class InformationFlowCheckerTest {
  @ParameterizedTest
  @ArgumentsSource(ExamplesProvider.class)
  void testRun(@ConvertWith(ImpAstParser.class) ProgramNode program) {
    final ProgramNode processedProgram = Elaborator.run(program);
    InformationFlowChecker.run(processedProgram);
  }
}
