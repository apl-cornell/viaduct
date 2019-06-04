package edu.cornell.cs.apl.viaduct.imp.ast;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.cornell.cs.apl.viaduct.ExamplesProvider;
import edu.cornell.cs.apl.viaduct.ImpAstParser;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.provider.ArgumentsSource;

class ImpAstNodeTest {
  @ParameterizedTest
  @ArgumentsSource(ExamplesProvider.class)
  void testEqualsIsReflexive(@ConvertWith(ImpAstParser.class) ImpAstNode ast) throws Exception {
    assertEquals(ast, ast);
  }
}
