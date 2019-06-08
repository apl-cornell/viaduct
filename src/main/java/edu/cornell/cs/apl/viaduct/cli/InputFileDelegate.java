package edu.cornell.cs.apl.viaduct.cli;

import com.beust.jcommander.Parameter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

/** Provides an input file parameter for commands. */
class InputFileDelegate {
  @Parameter(description = "<file>")
  private String input;

  File getInput() {
    return input != null ? new File(input) : null;
  }

  /** Create an efficient reader object from {@code input}. */
  Reader newInputReader() throws FileNotFoundException {
    InputStream stream = input == null ? System.in : new FileInputStream(input);
    return new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
  }
}
