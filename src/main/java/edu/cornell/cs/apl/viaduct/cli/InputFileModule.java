package edu.cornell.cs.apl.viaduct.cli;

import com.github.rvesse.airline.annotations.Arguments;
import com.github.rvesse.airline.annotations.restrictions.Once;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.imp.parser.Parser;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
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
    try (Reader reader = newInputReader()) {
      final String inputSource = getInput() == null ? "<stdin>" : getInput().getPath();
      return Parser.parse(reader, inputSource);
    }
  }

  /** Create an efficient {@link Reader} from the specified input file. */
  private Reader newInputReader() throws FileNotFoundException {
    final File file = getInput();
    InputStream stream = file == null ? System.in : new FileInputStream(file);
    return new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
  }
}
