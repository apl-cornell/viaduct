package edu.cornell.cs.apl.viaduct;

import com.github.rvesse.airline.parser.errors.ParseException;
import edu.cornell.cs.apl.viaduct.cli.CommandLineInterface;
import edu.cornell.cs.apl.viaduct.errors.CompilationError;
import org.fusesource.jansi.AnsiConsole;

public class Main {
  /** Run the compiler. */
  public static void main(String... args) {
    try {
      CommandLineInterface.parser().parse(args).call();
    } catch (Throwable e) {
      failWith(e);
    }
  }

  /**
   * Print a useful error message based on the exception. Then terminate with a non-zero exit code.
   */
  private static void failWith(Throwable e) {
    if (e instanceof ParseException) {
      // Invalid command-line arguments; print the problem and usage information.
      AnsiConsole.err.println(e.getLocalizedMessage());
      AnsiConsole.err.println();
      CommandLineInterface.usage(AnsiConsole.err);
    } else if (e instanceof CompilationError) {
      // User error; print short, pretty message.
      ((CompilationError) e).print(AnsiConsole.err());
    } else if (e instanceof RuntimeException) {
      // Developer error; give more detail.
      e.printStackTrace();
    } else {
      AnsiConsole.err.println(e.getLocalizedMessage());
    }
    System.exit(1);
  }
}
