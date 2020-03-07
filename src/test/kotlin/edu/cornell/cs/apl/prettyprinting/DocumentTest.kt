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

        @Test
        fun `nesting by 0 has no effect`() {
            (Document("first") / Document("second")).nested(0)
                .shouldPrintTo("first\nsecond")
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

    @Nested
    inner class Helpers {
        val empty: List<Document> = listOf()
        val single: List<Document> = listOf(Document("single"))
        val many: List<Document> = listOf(Document("1"), Document("2"), Document("3"))

        @Test
        fun `concatenated empty`() {
            empty.concatenated().shouldPrintTo("")
        }

        @Test
        fun `concatenated single`() {
            single.concatenated().shouldPrintTo("single")
        }

        @Test
        fun `concatenated many`() {
            many.concatenated().shouldPrintTo("123")
        }

        @Test
        fun `joined empty`() {
            empty.joined(prefix = Document("("), postfix = Document(")"))
                .shouldPrintTo("()")
        }

        @Test
        fun `joined single`() {
            single.joined(prefix = Document("("), postfix = Document(")"))
                .shouldPrintTo("(single)")
        }

        @Test
        fun `joined many`() {
            many.joined(prefix = Document("("), postfix = Document(")"))
                .shouldPrintTo("(1, 2, 3)")
        }

        @Test
        fun `tupled many`() {
            many.tupled().shouldPrintTo("(1, 2, 3)")
        }

        @Test
        fun `bracketed many`() {
            many.bracketed().shouldPrintTo("[1, 2, 3]")
        }

        @Test
        fun `braced many`() {
            many.braced().shouldPrintTo("{1, 2, 3}")
        }
    }
}

/** Asserts that this document is printed as [expected]. */
private fun Document.shouldPrintTo(expected: String) {
    assertEquals(expected, this.print())
}
