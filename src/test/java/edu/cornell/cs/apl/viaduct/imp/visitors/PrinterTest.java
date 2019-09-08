package edu.cornell.cs.apl.viaduct.imp.visitors;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.cornell.cs.apl.viaduct.ExamplesProvider;
import edu.cornell.cs.apl.viaduct.ImpAstParser;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.imp.parsing.Parser;
import edu.cornell.cs.apl.viaduct.imp.parsing.Printer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.provider.ArgumentsSource;

class PrinterTest {
  @ParameterizedTest
  @ArgumentsSource(ExamplesProvider.class)
  void testDualToParser(@ConvertWith(ImpAstParser.class) ProgramNode prog) throws Exception {
    final String printedAst = Printer.run(prog);
    assertEquals(prog, Parser.parse(printedAst));
  }
}
