package io.github.apl_cornell.viaduct.errors

import io.github.apl_cornell.viaduct.parsing.parse
import io.github.apl_cornell.viaduct.prettyprinting.DEFAULT_LINE_WIDTH
import io.github.apl_cornell.viaduct.prettyprinting.Document
import io.github.apl_cornell.viaduct.prettyprinting.plus
import io.github.apl_cornell.viaduct.syntax.surface.ProgramNode
import io.github.apl_cornell.viaduct.util.unicodeLineBreak
import org.apache.commons.text.WordUtils

/** Thrown when trying to compile a program with no host declarations. */
class NoHostDeclarationsError(override val source: String) : CompilationError() {
    override val category: String
        get() = "No Hosts"

    override val description: Document
        get() =
            Document(
                """
                This program has no host declarations.
                Hosts are the participants in the protocol.
                I cannot compile the program without any participants!
                """.reflow()
            ) + Document.lineBreak

    override val hint: Document
        get() = Document("Declare hosts and their authority labels like so:")
            .withData(exampleHostDeclarations)

    private companion object {
        val exampleHostDeclarations: ProgramNode
            get() = """
               host alice
               host trusted
            """.trimIndent().parse()

        /** Wrap words the given string. All new lines and indentation are removed. */
        // TODO: this should be implemented in [Document] since line width is determined only when printing.
        fun String.reflow(): String {
            val flattened =
                this.replace(unicodeLineBreak, " ")
                    .split(" ")
                    .filterNot { it.isBlank() }
                    .joinToString(" ")
            return WordUtils.wrap(flattened, DEFAULT_LINE_WIDTH)
        }
    }
}
