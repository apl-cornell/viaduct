package edu.cornell.cs.apl.viaduct.imp.parser;

import edu.cornell.cs.apl.viaduct.util.UnicodeUtil;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import org.apache.commons.io.IOUtils;

/**
 * Maintains metadata (for example, file path) about an input stream and gives access to its
 * contents.
 */
public class SourceFile {
  private final String path;
  private final String contents;

  /**
   * Starting offsets of each line. {@code lineOffsets[i]} is the number of {@link char}s (unicode
   * code units, not code points) from the beginning of the string to the beginning of line {@code
   * i}.
   */
  private final ArrayList<Integer> lineOffsets;

  private final ArrayList<String> lines;

  private SourceFile(String path, String contents) {
    this.path = Objects.requireNonNull(path);
    this.contents = Objects.requireNonNull(contents);

    // Compute line offsets
    String rest = contents;
    lineOffsets = new ArrayList<>();
    lineOffsets.add(0);
    lines = new ArrayList<>();
    while (true) {
      String[] splits = rest.split("\\R", 2);
      lines.add(splits[0]);
      if (splits.length == 2) {
        final int lastOffset = lineOffsets.get(lineOffsets.size() - 1);

        // Note: we cannot use {@code splits[0].length()} here since that does not include line
        // terminators.
        final int lineLength = rest.length() - splits[1].length();

        lineOffsets.add(lastOffset + lineLength);
        rest = splits[1];
      } else {
        // No more line terminators
        assert splits.length == 1;
        break;
      }
    }

    System.out.print("Line Offsets: ");
    System.out.println(lineOffsets);
  }

  /**
   * Construct a {@link SourceFile} by reading the contents of a file.
   *
   * @param source file to read
   */
  public static SourceFile from(File source) throws IOException {
    // NOTE: not using {@link BufferedStream} since {@link IOUtils.toString(Reader)} handles this.
    try (Reader reader =
        new InputStreamReader(new FileInputStream(source), StandardCharsets.UTF_8)) {
      return from(source.getPath(), reader);
    }
  }

  /**
   * Construct a {@link SourceFile} by consuming a reader.
   *
   * @param path description of where reader came from (e.g. file path, "stdin", etc.)
   * @param reader object to get the file contents from
   */
  public static SourceFile from(String path, Reader reader) throws IOException {
    final String contents = IOUtils.toString(reader);
    return new SourceFile(path, contents);
  }

  /** Return the number of {@link char}s (unicode code units) in the file. */
  public int length() {
    return contents.length();
  }

  /**
   * Return the number of lines in the file. This is equal to the number of line breaks plus 1. That
   * is, every file (including the empty one) has at least one line.
   */
  public int numberOfLines() {
    return lineOffsets.size();
  }

  /** Get the description of where the source file came from (e.g. a file path, "stdin", etc.). */
  public String getPath() {
    return path;
  }

  /** Returns a reader that can be used to access the contents of the file. */
  public Reader getReader() {
    return new StringReader(contents);
  }

  /**
   * Return the column number corresponding to the given offset. Columns are 1 indexed; for example,
   * offset 0 is on column 1.
   *
   * @param offset number of {@link char}s counting from the beginning of the file
   * @throws IndexOutOfBoundsException if offset is outside the file
   */
  int getColumn(int offset) {
    final int lineOffset = getLine(offset) - 1;
    final int columnOffset = offset - lineOffsets.get(lineOffset);

    final int column =
        1 + UnicodeUtil.countGraphemeClusters(lines.get(lineOffset).substring(0, columnOffset));

    assert 1 <= column;
    return column;
  }

  /**
   * Return the line number corresponding to the given offset. Lines are 1 indexed; for example,
   * offset 0 is on line 1.
   *
   * @param offset number of {@link char}s counting from the beginning of the file
   * @throws IndexOutOfBoundsException if offset is outside the file
   */
  int getLine(int offset) throws IndexOutOfBoundsException {
    if (offset < 0 || offset > contents.length()) {
      throw new IndexOutOfBoundsException();
    }
    final int index = Collections.binarySearch(lineOffsets, offset);
    final int line = index < 0 ? -(index + 1) : index + 1;

    assert 1 <= line && line <= numberOfLines();
    return line;
  }
}
