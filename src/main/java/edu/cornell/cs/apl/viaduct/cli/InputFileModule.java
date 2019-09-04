package edu.cornell.cs.apl.viaduct.cli;

import com.github.rvesse.airline.annotations.Arguments;
import com.github.rvesse.airline.annotations.restrictions.Once;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.imp.parser.Parser;
import edu.cornell.cs.apl.viaduct.imp.parser.SourceFile;
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
  private String input = null;

  /** Return the input file specified by the user, or {@code null} if none specified. */
  File getInput() {
    return input != null ? new File(input) : null;
  }

  /** Parse the input file and return the AST. */
  ProgramNode parse() throws Exception {
    return Parser.parse(newSourceFile());
  }

  /** Create a new {@link SourceFile} from the specified input file. */
  private SourceFile newSourceFile() throws IOException {
    final File file = getInput();
    if (file == null) {
      try (Reader reader = new InputStreamReader(System.in, StandardCharsets.UTF_8)) {
        return SourceFile.from("<stdin>", reader);
      }
    } else {
      return SourceFile.from(file);
    }
  }
}
