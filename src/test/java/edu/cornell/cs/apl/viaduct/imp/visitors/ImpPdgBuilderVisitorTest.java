package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.ExamplesProvider;
import edu.cornell.cs.apl.viaduct.ImpAstParser;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessName;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StatementNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.provider.ArgumentsSource;

class ImpPdgBuilderVisitorTest {
  @ParameterizedTest
  @ArgumentsSource(ExamplesProvider.class)
  void testGeneratePDG(@ConvertWith(ImpAstParser.class) ImpAstNode ast) {
    ProgramNode program = (ProgramNode) ast;
    StatementNode main = program.getProcessCode(ProcessName.getMain());
    main = new ImpPdgBuilderPreprocessVisitor().run(main);
    new ImpPdgBuilderVisitor().generatePDG(main);
  }
}
