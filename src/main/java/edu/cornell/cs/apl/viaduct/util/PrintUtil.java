package edu.cornell.cs.apl.viaduct.util;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.function.Consumer;
import org.fusesource.jansi.AnsiPrintStream;

public final class PrintUtil {
  /** Maximum number of characters to put on one line. */
  public static final int LINE_WIDTH = 80;

  /**
   * Capture the result of a color printer as a string. The printer is allowed to output ANSI color
   * codes, but these will be stripped before generating the string.
   *
   * @param printer a printer that generates output with ANSI color codes
   * @return output of the printer without color codes
   */
  public static String printToString(Consumer<PrintStream> printer) {
    final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
    try (PrintStream printStream =
        new AnsiPrintStream(new PrintStream(byteStream, false, Charset.defaultCharset()))) {
      printer.accept(printStream);
      return new String(byteStream.toByteArray(), Charset.defaultCharset());
    }
  }
}
