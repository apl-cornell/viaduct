package edu.cornell.cs.apl.prettyprinting

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class DocumentTest {
    @Nested
    inner class Empty {
        @Test
        fun `empty document is empty`() {
            Document().shouldPrintTo("")
        }

        @Test
        fun `empty + empty = empty`() {
            (Document() + Document()).shouldPrintTo("")
        }
    }

    @Nested
    inner class Concatenation {
        @Test
        fun `plus concatenates`() {
            (Document("hello") + "World").shouldPrintTo("helloWorld")
        }

        @Test
        fun `times adds a space`() {
            (Document("hello") * "world").shouldPrintTo("hello world")
        }

        @Test
        fun `empty + doc = doc`() {
            (Document() + "hello").shouldPrintTo("hello")
        }

        @Test
        fun `doc + empty = doc`() {
            (Document("hello") + Document()).shouldPrintTo("hello")
        }
    }

    @Nested
    inner class LineBreaking {
        @Test
        fun `lineBreak is a line break`() {
            Document.lineBreak.shouldPrintTo("\n")
            Document.forcedLineBreak.shouldPrintTo("\n")
        }

        @Test
        fun `multiple line breaks are preserved`() {
            (Document.lineBreak + Document.lineBreak).shouldPrintTo("\n\n")
        }

        @Test
        fun `div adds a line break`() {
            (Document("hello") / "world").shouldPrintTo("hello\nworld")
        }
    }

    @Nested
    inner class Nesting {
        @Test
        fun `nesting does not indent without line breaks`() {
            Document("first").nested(2).shouldPrintTo("first")
        }

        @Test
        fun `nesting indents after line break`() {
            listOf(Document("first"), Document("second"))
                .concatenated(Document.lineBreak)
                .nested(2)
                .shouldPrintTo("first\n  second")
        }

        @Test
        fun `nested nesting works`() {
            (Document("first") / (Document("second") / Document("third")).nested(2))
                .nested(2)
                .shouldPrintTo("first\n  second\n    third")
        }
    }

    @Nested
    inner class Grouping {
        @Test
        fun `grouping converts soft line breaks into spaces`() {
            (Document("hello") / "world").grouped().shouldPrintTo("hello world")
        }

        @Test
        fun `grouping does not change hard line breaks`() {
            (Document("hello") + Document.forcedLineBreak + "world")
                .grouped()
                .shouldPrintTo("hello\nworld")
        }
    }

    /** Assert that the document is printed as [expected]. */
    fun Document.shouldPrintTo(expected: String) {
        assertEquals(expected, this.print())
    }
}
