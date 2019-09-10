package edu.cornell.cs.apl.viaduct.imp.parsing;

import edu.cornell.cs.apl.viaduct.ExamplesProvider;
import java.io.File;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

class ParserTest {
  @ParameterizedTest
  @ArgumentsSource(ExamplesProvider.class)
  void testParse(File file) throws Exception {
    Parser.parse(SourceFile.from(file));
  }
}
