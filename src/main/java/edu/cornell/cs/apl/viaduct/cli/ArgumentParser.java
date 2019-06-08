package edu.cornell.cs.apl.viaduct.cli;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

public class ArgumentParser {
  private ArgumentParser() {}

  /** Parse the given arguments and return a command that will execute the specified task. */
  public static Command parse(String... argv) throws ParameterException {
    CommandMain main = new CommandMain();
    CommandFormat format = new CommandFormat();
    JCommander.newBuilder()
        .programName("viaduct")
        .addObject(main)
        .addCommand("format", format)
        .build()
        .parse(argv);
    return format;
  }
}
