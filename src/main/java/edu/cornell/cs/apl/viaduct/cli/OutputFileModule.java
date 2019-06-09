package edu.cornell.cs.apl.viaduct.cli;

import com.github.rvesse.airline.annotations.Option;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

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

  /** Create an efficient {@link Writer} to the specified output file. */
  BufferedWriter newOutputWriter() throws FileNotFoundException {
    final File file = getOutput();
    final OutputStream stream = file == null ? System.out : new FileOutputStream(file);
    return new BufferedWriter(new OutputStreamWriter(stream, StandardCharsets.UTF_8));
  }
}
