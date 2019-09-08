package edu.cornell.cs.apl.viaduct;

import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.imp.parsing.Parser;
import edu.cornell.cs.apl.viaduct.imp.parsing.SourceFile;
import java.io.File;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.params.converter.ArgumentConversionException;
import org.junit.jupiter.params.converter.ArgumentConverter;

/** Converts IMP source code paths to the parsed AST. */
public class ImpAstParser implements ArgumentConverter {
  @Override
  public ProgramNode convert(Object source, ParameterContext context)
      throws ArgumentConversionException {
    try {
      File path = (File) source;
      return Parser.parse(SourceFile.from(path));
    } catch (Exception e) {
      throw new ArgumentConversionException(e.getMessage());
    }
  }
}
