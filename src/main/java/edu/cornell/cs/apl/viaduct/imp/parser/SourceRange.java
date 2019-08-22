package edu.cornell.cs.apl.viaduct.imp.parser;

import com.google.auto.value.AutoValue;

/**
 * Denotes a set of consecutive characters in a source file. This set may be empty, in which case
 * the range corresponds to a single position.
 */
@AutoValue
public abstract class SourceRange {
  static SourceRange create(SourcePosition start, SourcePosition end) {
    assert start.getSourceFile().equals(end.getSourceFile());
    assert start.getOffset() <= end.getOffset();

    return new AutoValue_SourceRange(start, end);
  }

  /** Starting (leftmost) position. */
  public abstract SourcePosition getStart();

  /** End (rightmost) position. */
  public abstract SourcePosition getEnd();

  public final SourceFile getSourceFile() {
    return getStart().getSourceFile();
  }

  @Override
  public final String toString() {
    if (getStart().equals(getEnd())) {
      return getStart().toString();
    } else if (getStart().getLine() == getEnd().getLine()) {
      final String columns = getStart().getColumn() + "-" + getEnd().getColumn();
      return getSourceFile() + ":" + getStart().getLine() + ":" + columns;
    } else {
      final String start = getStart().getLine() + ":" + getStart().getColumn();
      final String end = getEnd().getLine() + ":" + getEnd().getColumn();
      return getSourceFile() + ":" + start + "-" + end;
    }
  }
}
