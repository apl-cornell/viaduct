package edu.cornell.cs.apl.viaduct.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands

class Viaduct :
    CliktCommand(help = "Compile high level specifications to secure distributed programs.") {
    init {
        subcommands(Format(), Compile())
        // TODO: Help, Interpret, Specification commands
    }

    override fun run() = Unit

    // Mark every unambiguous prefix of a command as an alias for that command.
    override fun aliases(): Map<String, List<String>> {
        val aliases: MutableMap<String, MutableSet<String>> = mutableMapOf()

        // TODO: use registeredSubcommands once that is made visible
        for (commandName in this.registeredSubcommandNames()) {
            commandName.prefixes().forEach { alias ->
                aliases.getOrPut(alias) { mutableSetOf() }.add(commandName)
            }
        }

        return aliases.filterValues { it.size == 1 }.mapValues { listOf(it.value.first()) }
    }
}

/** Returns all non-empty prefixes of [this]. */
private fun String.prefixes(): Iterable<String> =
    this.indices.map { this.substring(0..it) }
