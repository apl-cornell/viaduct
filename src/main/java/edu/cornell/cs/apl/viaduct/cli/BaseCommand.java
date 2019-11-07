package edu.cornell.cs.apl.viaduct.cli;

import java.io.IOException;
import java.util.concurrent.Callable;
import javax.inject.Inject;

/**
 * Base class for commands in the command-line interface.
 *
 * <p>Defines options and arguments common to all commands.
 */
abstract class BaseCommand implements Callable<Void> {
  @Inject InputFileModule input = new InputFileModule();

  @Inject OutputDirModule output = new OutputDirModule();

  /** Execute the command. */
  abstract void run() throws IOException;

  @Override
  public final Void call() throws IOException {
    run();
    return null;
  }
}
