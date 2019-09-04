package edu.cornell.cs.apl.viaduct;

import com.github.rvesse.airline.parser.errors.ParseException;
import edu.cornell.cs.apl.viaduct.cli.CommandLineInterface;
import edu.cornell.cs.apl.viaduct.errors.CompilationError;
import java.util.concurrent.Callable;
import org.fusesource.jansi.AnsiConsole;

public class Main {
  /** Run the compiler. */
  public static void main(String... args) {
    try {
      final Callable<Void> command = CommandLineInterface.parser().parse(args);
      command.call();
    } catch (Throwable e) {
      failWith(e);
    }
  }

  /**
   * Print a useful error message based on the exception and terminate with a non-zero exit code.
   */
  private static void failWith(Throwable e) {
    if (e instanceof ParseException) {
      // Invalid command-line arguments; print the problem and usage information.
      System.err.println(e.getLocalizedMessage());
      System.err.println();
      CommandLineInterface.usage(System.err);
    } else if (e instanceof CompilationError) {
      // User error; print short, pretty message.
      ((CompilationError) e).print(AnsiConsole.err());
    } else if (e instanceof RuntimeException) {
      // Indicates developer error; give more detail.
      e.printStackTrace();
    } else {
      System.err.println(e.getLocalizedMessage());
    }
    System.exit(1);
  }
}
