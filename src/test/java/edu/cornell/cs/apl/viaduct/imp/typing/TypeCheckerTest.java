package edu.cornell.cs.apl.viaduct.imp.typing;

import edu.cornell.cs.apl.viaduct.ExamplesProvider;
import edu.cornell.cs.apl.viaduct.ImpAstParser;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.provider.ArgumentsSource;

class TypeCheckerTest {
  @ParameterizedTest
  @ArgumentsSource(ExamplesProvider.class)
  void testTypeChecker(@ConvertWith(ImpAstParser.class) ProgramNode program) {
    TypeChecker.run(program);
  }
}
