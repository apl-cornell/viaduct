package edu.cornell.cs.apl.viaduct.cli

import com.github.ajalt.clikt.core.PrintHelpMessage
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class CommandLineInterfaceTest {
    private val example = "tests/should-pass/Local.via"

    @Test
    fun help() {
        assertThrows<PrintHelpMessage> { cli("--help") }
        assertThrows<PrintHelpMessage> { cli("format", "--help") }
    }

    @Test
    fun `format command`() {
        cli("format", example)
        cli("format", "--check", example)
        cli("format", "--elaborate", example)
    }

    @Test
    fun `compile command`() {
        cli("compile", example)
    }

    @Test
    fun `specification command`() {
        cli("specification", "--adversary", "A", example)
    }

    @Test
    fun `no arguments causes error`() {
        assertThrows<PrintHelpMessage> { cli() }
    }
}

/** Calls the command line interface with [arguments]. */
private fun cli(vararg arguments: String) =
    Viaduct().parse(arrayOf(*arguments))
