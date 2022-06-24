package edu.cornell.cs.apl.viaduct.parsing

/**
 * Represents a position in a source file. Positions correspond to spaces between characters rather
 * than characters themselves.
 *
 * In a file with n characters, there are n + 1 positions: one before
 * the first character, one after the last character, and n - 1 between each consecutive character.
 *
 * @param offset Number of [Char]s (unicode code units, not code points) before this position,
 *     counting from the beginning of the file. Starts at 0.
 */
data class SourcePosition(internal val sourceFile: SourceFile, val offset: Int) :
    Comparable<SourcePosition> {
    init {
        require(offset in 0..sourceFile.length) { "Offset must be in the file." }
    }

    /** Line number of this position. 1 indexed. */
    val line: Int
        get() = sourceFile.getLineNumber(offset)

    /**
     * Column number of the character that comes *after* this position. 1 indexed.
     *
     * Note that even though each position has a line number, no position has a column number.
     * This is because columns correspond to characters not the spaces between them.
     */
    val column: Int
        get() = sourceFile.getColumnNumber(offset)

    /** Description of where the source file came from.  */
    val sourcePath: String
        get() = sourceFile.path

    override fun compareTo(other: SourcePosition): Int {
        return offset.compareTo(other.offset)
    }

    override fun toString(): String {
        return "$sourcePath:$line:$column"
    }
}
