package edu.cornell.cs.apl.viaduct.cli;

import com.github.rvesse.airline.annotations.Arguments;
import com.github.rvesse.airline.annotations.restrictions.Once;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.imp.parsing.Parser;
import edu.cornell.cs.apl.viaduct.imp.parsing.SourceFile;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

/** Provides an input file argument for commands. */
final class InputFileModule {
  @Arguments(title = "file", description = "Read input from <file> (default: stdin)")
  @Once
  // TODO: this generates a badly-formatted huge dump of text in help. Enable when fixed.
  // @com.github.rvesse.airline.annotations.restrictions.File(mustExist = true, writable = false)
  protected String input = null;

  /** Parse the input file and return the AST. */
  ProgramNode parse() throws IOException {
    return Parser.parse(newSourceFile());
  }

  /** Create a new {@link SourceFile} from the specified input file. */
  private SourceFile newSourceFile() throws IOException {
    if (input == null) {
      try (Reader reader = new InputStreamReader(System.in, StandardCharsets.UTF_8)) {
        return SourceFile.from("<stdin>", reader);
      }
    } else {
      return SourceFile.from(new File(input));
    }
  }
}
