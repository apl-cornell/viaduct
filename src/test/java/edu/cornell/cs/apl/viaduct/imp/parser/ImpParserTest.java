package edu.cornell.cs.apl.viaduct.imp.parser;

import edu.cornell.cs.apl.viaduct.ExamplesProvider;
import java.io.File;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

class ImpParserTest {
  @ParameterizedTest
  @ArgumentsSource(ExamplesProvider.class)
  void testParse(File source) throws Exception {
    Parser.parse(source);
  }
}
