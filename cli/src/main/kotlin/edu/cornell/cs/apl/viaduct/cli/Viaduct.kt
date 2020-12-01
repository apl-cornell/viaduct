package edu.cornell.cs.apl.viaduct.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.versionOption
import edu.cornell.cs.apl.viaduct.version

class Viaduct : CliktCommand(help = "Compile high level specifications to secure distributed programs.") {
    val verbose by verbosity()

    init {
        versionOption(version)
        subcommands(Format(), Compile(), Specification(), Run())
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
