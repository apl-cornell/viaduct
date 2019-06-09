package edu.cornell.cs.apl.viaduct.cli;

import java.util.concurrent.Callable;
import javax.inject.Inject;

/**
 * Base class for commands in the command-line interface.
 *
 * <p>Defines options and arguments common to all commands.
 */
abstract class BaseCommand implements Runnable, Callable<Void> {
  @Inject InputFileModule input = new InputFileModule();

  @Inject OutputFileModule output = new OutputFileModule();

  @Override
  public final void run() {
    try {
      call();
    } catch (Exception e) {
      throw new Error(e);
    }
  }
}
