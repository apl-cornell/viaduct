package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.ExamplesProvider;
import edu.cornell.cs.apl.viaduct.ImpAstParser;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessName;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StatementNode;
import edu.cornell.cs.apl.viaduct.imp.transformers.AnfConverter;
import edu.cornell.cs.apl.viaduct.imp.transformers.Elaborator;
import edu.cornell.cs.apl.viaduct.imp.transformers.ImpPdgBuilderPreprocessor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.provider.ArgumentsSource;

class ImpPdgBuilderVisitorTest {
  @ParameterizedTest
  @ArgumentsSource(ExamplesProvider.class)
  void testGeneratePDG(@ConvertWith(ImpAstParser.class) ProgramNode program) {
    final ProgramNode processesProgram =
        ImpPdgBuilderPreprocessor.run(AnfConverter.run(Elaborator.run(program)));
    final StatementNode main = processesProgram.processes().get(ProcessName.getMain()).getBody();
    new ImpPdgBuilderVisitor().generatePDG(main);
  }
}
