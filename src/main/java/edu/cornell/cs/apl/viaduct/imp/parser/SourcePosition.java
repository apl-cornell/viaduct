package edu.cornell.cs.apl.viaduct.imp.parser;

import com.google.auto.value.AutoValue;

/**
 * Represents a position in a source file. Positions correspond to spaces between characters rather
 * than characters themselves. In a file with n characters, there are n + 1 positions: one before
 * the first character, one after the last character, and n - 1 between each consecutive character.
 */
@AutoValue
public abstract class SourcePosition implements Comparable<SourcePosition> {
  static SourcePosition create(SourceFile sourceFile, int offset) {
    assert 0 <= offset && offset <= sourceFile.length();
    return new AutoValue_SourcePosition(sourceFile, offset);
  }

  abstract SourceFile getSourceFile();

  /**
   * Number of {@link char}s (unicode code units, not code points) before this position, counting
   * from the beginning of file. Starts at 0.
   */
  public abstract int getOffset();

  /** Line number of this position. 1 indexed. */
  public final int getLine() {
    return getSourceFile().getLine(getOffset());
  }

  /** Column number of this position. 1 indexed. */
  public final int getColumn() {
    return getSourceFile().getColumn(getOffset());
  }

  public final String getSourcePath() {
    return getSourceFile().getPath();
  }

  @Override
  public int compareTo(SourcePosition that) {
    return Integer.compare(this.getOffset(), that.getOffset());
  }

  @Override
  public final String toString() {
    return getSourceFile() + ":" + getLine() + ":" + getColumn();
  }
}
