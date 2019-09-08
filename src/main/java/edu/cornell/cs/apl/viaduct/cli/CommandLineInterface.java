package edu.cornell.cs.apl.viaduct.cli;

import com.github.rvesse.airline.annotations.Alias;
import com.github.rvesse.airline.annotations.Cli;
import com.github.rvesse.airline.annotations.Parser;
import com.github.rvesse.airline.annotations.restrictions.global.CommandRequired;
import com.github.rvesse.airline.annotations.restrictions.global.NoUnexpectedArguments;
import com.github.rvesse.airline.help.Help;
import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.concurrent.Callable;

/**
 * Defines the command-line interface of the application.
 *
 * <p>Provides a parser for command-line arguments, and a way to execute the tasks determined by
 * them.
 */
@Cli(
    name = "viaduct",
    description = "Extensible optimizing MPC compiler",
    commands = {Help.class, FormatCommand.class, InterpretCommand.class, CompileCommand.class},
    parserConfiguration =
        @Parser(
            allowCommandAbbreviation = true,
            aliases = {
              @Alias(
                  name = "-h",
                  arguments = {"help"}),
              @Alias(
                  name = "--help",
                  arguments = {"help"})
            }))
@NoUnexpectedArguments
@CommandRequired
public class CommandLineInterface {
  private static final com.github.rvesse.airline.Cli<Callable<Void>> PARSER =
      new com.github.rvesse.airline.Cli<>(CommandLineInterface.class);

  /**
   * Return the command line parser.
   *
   * <p>Calling {@link com.github.rvesse.airline.Cli#parse(String...)} with command-line arguments
   * will return a {@link Callable} that will execute the task determined by the arguments.
   */
  public static com.github.rvesse.airline.Cli<Callable<Void>> parser() {
    return PARSER;
  }

  /**
   * Print usage information to the given output stream.
   *
   * @param output output stream to print usage information
   */
  public static void usage(PrintStream output) {
    try {
      Help.help(parser().getMetadata(), new LinkedList<>(), output);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
