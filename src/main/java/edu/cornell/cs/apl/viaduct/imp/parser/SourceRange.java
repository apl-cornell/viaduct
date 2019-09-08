package edu.cornell.cs.apl.viaduct.imp.parser;

import com.google.auto.value.AutoValue;
import org.apache.commons.lang3.ObjectUtils;

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
    return getStart().getSourcePath();
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
      final String start = getStart().getLine() + ":" + startColumn;
      final String end = getEnd().getLine() + ":" + endColumn;
      return getSourcePath() + ":" + start + "-" + end;
    }
  }
}
