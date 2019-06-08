package edu.cornell.cs.apl.viaduct.cli;

import com.beust.jcommander.Parameter;

/** Base command. Adds parameter for printing usage information. */
class CommandMain {
  @Parameter(
      names = {"-h", "--help"},
      description = "Display available options",
      help = true)
  private boolean help;

  boolean getHelp() {
    return help;
  }
}
