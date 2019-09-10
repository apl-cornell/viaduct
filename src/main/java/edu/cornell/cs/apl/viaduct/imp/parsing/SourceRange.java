package edu.cornell.cs.apl.viaduct.imp.parsing;

import com.google.auto.value.AutoValue;
import java.io.PrintStream;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;

/**
 * Denotes a nonempty set of consecutive characters in a source file. Empty ranges are not allowed,
 * since there is no column number that corresponds to a single position.
 */
@AutoValue
public abstract class SourceRange {
  /**
   * Create a range given the positions that surround a sequence of characters.
   *
   * @param start position that comes just before the characters in the range
   * @param end position that comes just after the characters in the range
   */
  static SourceRange create(SourcePosition start, SourcePosition end) {
    if (!start.getSourceFile().equals(end.getSourceFile())) {
      throw new IllegalArgumentException("Positions lie in different files.");
    }
    if (start.equals(end)) {
      throw new IllegalArgumentException("Empty source range.");
    }
    if (start.getOffset() > end.getOffset()) {
      throw new IllegalArgumentException("Start position comes after the end position.");
    }

    return new AutoValue_SourceRange(start, end);
  }

  /** Start position. Comes just before the characters in the range. */
  public abstract SourcePosition getStart();

  /** End position. Comes just after the characters in the range. */
  public abstract SourcePosition getEnd();

  /** Description of where the source file came from. */
  public final String getSourcePath() {
    return getSourceFile().getPath();
  }

  private SourceFile getSourceFile() {
    return getStart().getSourceFile();
  }

  /** Combine two ranges to create a range that spans both. */
  public final SourceRange merge(SourceRange that) {
    final SourcePosition start = ObjectUtils.min(this.getStart(), that.getStart());
    final SourcePosition end = ObjectUtils.max(this.getEnd(), that.getEnd());
    return SourceRange.create(start, end);
  }

  @Override
  public final String toString() {
    final int startColumn = getStart().getColumn();
    final int endColumn = getEnd().getColumn() - 1;

    if (getStart().getLine() == getEnd().getLine()) {
      final String columns = startColumn + "-" + endColumn;
      return getSourcePath() + ":" + getStart().getLine() + ":" + columns;
    } else {
      final String start = "(" + getStart().getLine() + ":" + startColumn + ")";
      final String end = "(" + getEnd().getLine() + ":" + endColumn + ")";
      return getSourcePath() + ":" + start + "-" + end;
    }
  }

  /**
   * Print the relevant portions of the source file and highlight the region that corresponds to
   * this location.
   *
   * @param output where to print the output
   * @param contextLines number of lines before and after the relevant region to display to give
   *     more context to the user
   */
  public void showInSource(PrintStream output, int contextLines) {
    final int startLine = getStart().getLine();
    final int endLine = getEnd().getLine();

    // Number of characters it takes to represent the largest line number.
    final int lineNumberWidth = Integer.toString(endLine).length();

    // True if we are highlighting multiple lines; false otherwise.
    final boolean multiLineMode = startLine != endLine;

    // Print relevant lines
    final int firstLine = Math.max(1, startLine - contextLines);
    final int lastLine = Math.min(getSourceFile().numberOfLines(), endLine + contextLines);
    for (int line = firstLine; line <= lastLine; line++) {
      final String lineNumber = String.format("%" + lineNumberWidth + "d|", line);
      final boolean highlight = startLine <= line && line <= endLine;

      // Print line number
      output.print(lineNumber);

      // Print multiline error indicator
      if (multiLineMode) {
        if (highlight) {
          output.print(Ansi.ansi().fg(Color.RED).a('>').reset());
        } else {
          output.print(" ");
        }
      }

      // Print space between line numbers and line contents
      output.print(" ");

      // Print the actual line
      output.println(getSourceFile().getLine(line));

      // Print error indicator for single-line mode
      if (highlight) {
        final int startColumn = getStart().getColumn();
        final int endColumn = getEnd().getColumn();
        final int highlightStartColumn = lineNumber.length() + 1 + startColumn;
        final int highlightLength = endColumn - startColumn;
        output.print(StringUtils.repeat(' ', highlightStartColumn - 1));
        output.println(
            Ansi.ansi().fg(Color.RED).a(StringUtils.repeat('^', highlightLength)).reset());
      }
    }

    // Make sure there is uniform vertical space after the displayed source code.
    if (multiLineMode || contextLines > 0) {
      // Last line did not have an underline. Add blank line instead.
      output.println();
    }
  }

  /** Similar as {@link #showInSource(PrintStream, int)}, but with context set to a good default. */
  public void showInSource(PrintStream output) {
    final int contextLines = getStart().getLine() == getEnd().getLine() ? 0 : 1;
    showInSource(output, contextLines);
  }
}
