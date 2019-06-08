package edu.cornell.cs.apl.viaduct.cli;

import com.beust.jcommander.Parameter;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

/** Provides an output file parameter for commands. */
class OutputFileDelegate {
  @Parameter(
      names = {"-o", "--output"},
      description = "Output file (default: stdout)")
  File output;

  /** Create an efficient writer object from {@code output}. */
  Writer newOutputWriter() throws FileNotFoundException {
    OutputStream stream = output == null ? System.out : new FileOutputStream(output);
    return new BufferedWriter(new OutputStreamWriter(stream, StandardCharsets.UTF_8));
  }
}
