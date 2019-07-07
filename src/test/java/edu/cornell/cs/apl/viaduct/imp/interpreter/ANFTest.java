package edu.cornell.cs.apl.viaduct.imp.interpreter;

import edu.cornell.cs.apl.viaduct.ExamplesProvider;
import edu.cornell.cs.apl.viaduct.ImpAstParser;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessName;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.imp.visitors.ANFVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.ElaborationVisitor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.provider.ArgumentsSource;

class ANFTest {
  @ParameterizedTest
  @ArgumentsSource(ExamplesProvider.class)
  void testRun(@ConvertWith(ImpAstParser.class) ImpAstNode ast) {
    // ensure that A-normal form translation is semantics preserving.

    ANFVisitor anfRewriter = new ANFVisitor();
    ElaborationVisitor elaborator = new ElaborationVisitor();

    ProgramNode config = (ProgramNode)ast;
    config = elaborator.run(config);

    ProgramNode anfConfig = anfRewriter.run(config);
    anfConfig = elaborator.run(anfConfig);

    Map<ProcessName, Store> results = Interpreter.run(config);
    Map<ProcessName, Store> anfResults = Interpreter.run(anfConfig);

    assertEquals(results, anfResults);
  }
}
