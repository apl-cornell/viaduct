package edu.cornell.cs.apl.viaduct.imp.transformers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.cornell.cs.apl.viaduct.ExamplesProvider;
import edu.cornell.cs.apl.viaduct.ImpAstParser;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessName;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.imp.interpreter.Interpreter;
import edu.cornell.cs.apl.viaduct.imp.interpreter.Store;
import edu.cornell.cs.apl.viaduct.imp.parsing.Printer;

import java.util.Map;
import java.util.Map.Entry;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.provider.ArgumentsSource;

class AnfConverterTest {
  @ParameterizedTest
  @ArgumentsSource(ExamplesProvider.class)
  void testIdempotent(@ConvertWith(ImpAstParser.class) ProgramNode ast) {
    final ProgramNode program = Elaborator.run(ast);
    final ProgramNode anfProgram = AnfConverter.run(program);
    final ProgramNode anfAnfProgram = AnfConverter.run(anfProgram);

    System.out.println("ANF once:");
    Printer.run(anfProgram, System.out, false);

    System.out.println("\nANF twice:");
    Printer.run(anfAnfProgram, System.out, false);

    assertEquals(anfProgram, anfAnfProgram);
  }

  @DisplayName("ANF translation must preserve dynamic semantics.")
  @ParameterizedTest
  @ArgumentsSource(ExamplesProvider.class)
  void testPreservesSemantics(@ConvertWith(ImpAstParser.class) ProgramNode ast) {
    final ProgramNode program = Elaborator.run(ast);
    final ProgramNode anfProgram = AnfConverter.run(program);

    Map<ProcessName, Store> results = Interpreter.run(program);
    Map<ProcessName, Store> anfResults = Interpreter.run(anfProgram);

    for (Entry<ProcessName, Store> entry : results.entrySet()) {
      final Store result = entry.getValue();
      final Store anfResult = anfResults.get(entry.getKey());
      assertTrue(result.agreesWith(anfResult, result.variableSet()));
    }
  }
}
