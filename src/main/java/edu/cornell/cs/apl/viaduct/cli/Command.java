package edu.cornell.cs.apl.viaduct.cli;

/**
 * Used for parsing command-line arguments and executing the task they determine.
 *
 * <p>The command-line parser returns an object implementing this interface. Calling {@link #run()}
 * on this objects executes the task determined by the command-line arguments.
 */
public interface Command {
  /** Execute the task determined by the command-line arguments. */
  void run() throws Exception;
}
