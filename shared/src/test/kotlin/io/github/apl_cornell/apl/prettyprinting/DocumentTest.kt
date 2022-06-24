package io.github.apl_cornell.apl.prettyprinting

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class DocumentTest {
    private val nl = System.lineSeparator()

    @Test
    fun `toString is not supported`() {
        assertThrows<UnsupportedOperationException> { Document().toString() }
    }

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
            Document.lineBreak.shouldPrintTo(nl)
            Document.forcedLineBreak.shouldPrintTo(nl)
        }

        @Test
        fun `multiple line breaks are preserved`() {
            (Document.lineBreak + Document.lineBreak).shouldPrintTo("$nl$nl")
        }

        @Test
        fun `div adds a line break`() {
            (Document("hello") / "world").shouldPrintTo("hello${nl}world")
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
                .shouldPrintTo("first$nl  second")
        }

        @Test
        fun `nested nesting works`() {
            (Document("first") / (Document("second") / Document("third")).nested(2))
                .nested(2)
                .shouldPrintTo("first$nl  second$nl    third")
        }

        @Test
        fun `nesting by 0 has no effect`() {
            (Document("first") / Document("second")).nested(0)
                .shouldPrintTo("first${nl}second")
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
                .shouldPrintTo("hello${nl}world")
        }
    }

    @Nested
    inner class Helpers {
        private val empty: List<Document> = listOf()
        private val single: List<Document> = listOf(Document("single"))
        private val many: List<Document> = listOf(Document("1"), Document("2"), Document("3"))

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

    @Nested
    inner class Unicode {
        private fun String.shouldPrintToItself() =
            Document(this).shouldPrintTo(this)

        @Test
        fun `BMP Unicode characters (1 UTF-16 code unit)`() {
            "⊤".shouldPrintToItself()
            "⊥".shouldPrintToItself()
            "∨".shouldPrintToItself()
            "∧".shouldPrintToItself()
            "⊔".shouldPrintToItself()
            "⊓".shouldPrintToItself()
            "←".shouldPrintToItself()
            "→".shouldPrintToItself()
        }

        @Test
        fun `SMP Unicode characters (2 UTF-16 code units)`() {
            "\uD83D\uDC4D".shouldPrintToItself()
        }

        @Test
        fun `grapheme clusters`() {
            "\uD83D\uDC4D\uD83C\uDFFF".shouldPrintToItself()
        }
    }

    @Nested
    /** Note: unfortunately, all of these must be visually inspected. */
    inner class AnsiStyling {
        private fun Document.setForegroundColor(color: AnsiColor) =
            this.styled(object : Style {
                override val foregroundColor: AnsiColor
                    get() = color
            })

        private fun Document.setBackgroundColor(color: AnsiColor) =
            this.styled(object : Style {
                override val backgroundColor: AnsiColor
                    get() = color
            })

        @Test
        fun `default foreground color`() {
            Document("default").setForegroundColor(DefaultColor).show()
        }

        @Test
        fun `normal foreground colors`() {
            AnsiBaseColor.values().map { color ->
                Document(color.name.lowercase()).setForegroundColor(NormalColor(color))
            }.concatenated(separator = Document(" ")).show()
        }

        @Test
        fun `bright foreground colors`() {
            AnsiBaseColor.values().map { color ->
                Document(color.name.lowercase()).setForegroundColor(BrightColor(color))
            }.concatenated(separator = Document(" ")).show()
        }

        @Test
        fun `default background color`() {
            Document("default").setBackgroundColor(DefaultColor).show()
        }

        @Test
        fun `normal background colors`() {
            AnsiBaseColor.values().map { color ->
                Document(color.name.lowercase()).setBackgroundColor(NormalColor(color))
            }.concatenated(separator = Document(" ")).show()
        }

        @Test
        fun `bright background colors`() {
            AnsiBaseColor.values().map { color ->
                Document(color.name.lowercase()).setBackgroundColor(BrightColor(color))
            }.concatenated(separator = Document(" ")).show()
        }

        @Test
        fun `font styles`() {
            (
                Document("italic").styled(Italic) *
                    Document("bold").styled(Bold) *
                    Document("underline").styled(Underline)
                ).show()
        }
    }
}

/** Asserts that this document is printed as [expected]. */
private fun Document.shouldPrintTo(expected: String) {
    assertEquals(expected, this.print())
}

/** Prints [this] document for user inspection. */
private fun Document.show() =
    (this + Document.lineBreak).print(System.out, ansi = true)

private object Italic : Style {
    override val italic: Boolean
        get() = true
}

private object Bold : Style {
    override val bold: Boolean
        get() = true
}

private object Underline : Style {
    override val underline: Boolean
        get() = true
}
