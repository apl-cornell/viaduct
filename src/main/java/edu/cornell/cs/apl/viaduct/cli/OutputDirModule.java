package edu.cornell.cs.apl.viaduct.cli;

import com.github.rvesse.airline.annotations.Option;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.fusesource.jansi.AnsiConsole;
import org.fusesource.jansi.AnsiPrintStream;

/** Provides an output file option for commands. */
final class OutputDirModule {
  @Option(
      name = {"-o", "--output"},
      title = "dir",
      description = "Write output files to <dir> (default: stdout)")
  // TODO: these generate huge dumps of text in help. Enable when fixed.
  // @Once
  // @com.github.rvesse.airline.annotations.restrictions.File(readable = false)
  protected String outputDir = null;

  /** Create a {@link PrintStream} (which expects ANSI color codes) to the specified output file. */
  PrintStream newOutputStream(String name) throws IOException {
    if (outputDir == null) {
      // Read from standard input.
      return AnsiConsole.out();
    } else {
      // TODO: PrintStream doesn't throw errors when writing. These will fail silently.
      return
          new AnsiPrintStream(
              new PrintStream(
                  new File(outputDir, name), StandardCharsets.UTF_8));
    }
  }
}
