package edu.cornell.cs.apl.viaduct.cli;

import com.github.rvesse.airline.annotations.Option;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.fusesource.jansi.AnsiConsole;
import org.fusesource.jansi.AnsiPrintStream;

/** Provides an output file option for commands. */
final class OutputFileModule {
  @Option(
      name = {"-o", "--output"},
      title = "file",
      description = "Write output to <file> (default: stdout)")
  // TODO: these generate huge dumps of text in help. Enable when fixed.
  // @Once
  // @com.github.rvesse.airline.annotations.restrictions.File(readable = false)
  private String output = null;

  /** Return the output file specified by the user, or {@code null} if none specified. */
  File getOutput() {
    return output != null ? new File(output) : null;
  }

  /** Create a {@link PrintStream} that expects ANSI color codes to the specified output file. */
  PrintStream newOutputStream() throws IOException {
    final File file = getOutput();
    if (file == null) {
      return AnsiConsole.out();
    } else {
      // TODO: PrintStream doesn't throw errors when writing. These will fail silently.
      return new AnsiPrintStream(new PrintStream(file, StandardCharsets.UTF_8));
    }
  }
}
