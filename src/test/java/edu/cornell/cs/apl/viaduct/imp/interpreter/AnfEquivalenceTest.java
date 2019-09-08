package edu.cornell.cs.apl.viaduct.imp.interpreter;

import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.cornell.cs.apl.viaduct.ExamplesProvider;
import edu.cornell.cs.apl.viaduct.ImpAstParser;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessName;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.imp.transformers.AnfConverter;
import edu.cornell.cs.apl.viaduct.imp.transformers.Elaborator;
import java.util.Map;
import java.util.Map.Entry;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.provider.ArgumentsSource;

class AnfEquivalenceTest {
  @ParameterizedTest
  @ArgumentsSource(ExamplesProvider.class)
  void testRun(@ConvertWith(ImpAstParser.class) ProgramNode ast) {
    // Ensure that A-normal form translation is semantics preserving.

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
