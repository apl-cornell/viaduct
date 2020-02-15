package edu.cornell.cs.apl.viaduct.parsing

import edu.cornell.cs.apl.viaduct.util.graphemeClusterCount
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import java.io.File
import java.io.Reader
import java.io.StringReader

/**
 * Maintains metadata (for example, file path) about an input stream and gives access to its
 * contents.
 *
 * @param path Description of where the source file came from (e.g. a file path, "stdin", etc.).
 * @param contents File contents.
 */
class SourceFile private constructor(val path: String, private val contents: String) {
    /**
     * Starting offsets of each line.
     *
     * `lineOffsets.get(i)` is the number of [Char]s (unicode code units, not code points) from the
     * beginning of [contents] to the beginning of line `i`.
     */
    private val lineOffsets: ImmutableList<Int>

    /** Lines in [contents] without line breaks. */
    private val lines: ImmutableList<String>

    init {
        // Initialize [lineOffsets] and [lines].
        var remaining: String = contents
        val lineOffsets: MutableList<Int> = mutableListOf(0)
        val lines: MutableList<String> = mutableListOf()
        while (true) {
            val splits = remaining.split(Regex("\\R"), limit = 2)
            lines.add(splits[0])
            if (splits.size == 2) {
                val lastOffset = lineOffsets[lineOffsets.size - 1]
                // Note: we cannot use `splits[0].length()` here
                // since that does not include line terminators.
                val lineLength = remaining.length - splits[1].length
                lineOffsets.add(lastOffset + lineLength)
                remaining = splits[1]
            } else {
                // No more line terminators
                assert(splits.size == 1)
                break
            }
        }

        assert(lineOffsets.size == lines.size)
        this.lineOffsets = lineOffsets.toImmutableList()
        this.lines = lines.toImmutableList()
    }

    /** Returns a new [Reader] for accessing the contents of the file.  */
    fun createReader(): Reader {
        return StringReader(contents)
    }

    /** Number of [Char]s (unicode code units) in the file.  */
    val length: Int
        get() = contents.length

    /**
     * Number of lines in the file.
     *
     * This is equal to the number of line breaks plus 1.
     * That is, every file (including the empty one) has at least one line.
     */
    val numberOfLines: Int
        get() = lines.size

    /**
     * Returns the column number corresponding to the given offset.
     * Columns are 1 indexed; for example, offset 0 is on column 1.
     *
     * @param offset number of [Char]s counting from the beginning of the file
     * @throws IndexOutOfBoundsException if offset is outside the file
     */
    internal fun getColumnNumber(offset: Int): Int {
        val line = getLineNumber(offset)
        val columnOffset = offset - lineOffsets[line - 1]
        val column = 1 + getLine(line).substring(0, columnOffset).graphemeClusterCount()
        assert(1 <= column)
        return column
    }

    /**
     * Returns the line number corresponding to the given offset.
     * Lines are 1 indexed; for example, offset 0 is on line 1.
     *
     * @param offset number of [Char]s counting from the beginning of the file
     * @throws IndexOutOfBoundsException if offset is outside the file
     */
    internal fun getLineNumber(offset: Int): Int {
        if (offset !in 0..contents.length) {
            throw IndexOutOfBoundsException()
        }
        val index = lineOffsets.binarySearch(offset)
        val line = if (index < 0) -(index + 1) else index + 1
        assert(line in 1..numberOfLines)
        return line
    }

    /**
     * Returns the contents of the given line. The result will not contain any line breaks.
     *
     * Note that this is 1 indexed to match [getLineNumber].
     */
    internal fun getLine(lineNumber: Int): String {
        return lines[lineNumber - 1]
    }

    companion object {
        /**
         * Constructs a [SourceFile] by reading the contents of a file.
         *
         * @param source file to read
         */
        @JvmStatic
        fun from(source: File): SourceFile {
            return SourceFile(source.path, source.readText())
        }

        /**
         * Constructs a [SourceFile] from a string.
         *
         * @param path Description of where the string came from (e.g. file path, "stdin", etc.)
         * @param contents File contents.
         */
        @JvmStatic
        fun from(path: String, contents: String): SourceFile {
            return SourceFile(path, contents)
        }

        /**
         * Constructs a [SourceFile] by consuming a reader.
         *
         * @param path Description of where reader came from (e.g. file path, "stdin", etc.)
         * @param reader Object to read file contents from
         */
        @JvmStatic
        fun from(path: String, reader: Reader): SourceFile {
            return SourceFile(path, reader.readText())
        }
    }
}
