package edu.cornell.cs.apl.viaduct.cli;

import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.imp.parser.Parser;
import edu.cornell.cs.apl.viaduct.imp.visitors.PrintVisitor;
import java.io.Reader;
import java.io.Writer;

@Parameters(commandDescription = "Pretty print code")
class CommandFormat implements Command {
  @ParametersDelegate private InputFileDelegate inputFile = new InputFileDelegate();

  @ParametersDelegate private OutputFileDelegate outputFile = new OutputFileDelegate();

  @Override
  public void run() throws Exception {
    try (Reader reader = inputFile.newInputReader()) {
      final String inputSource =
          inputFile.getInput() == null ? "<stdin>" : inputFile.getInput().getPath();
      final ImpAstNode program = Parser.parse(reader, inputSource);
      try (Writer writer = outputFile.newOutputWriter()) {
        writer.write(new PrintVisitor().run(program));
        writer.write('\n');
      }
    }
  }
}
