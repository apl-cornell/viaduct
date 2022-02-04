package edu.cornell.cs.apl.viaduct.cli

import com.github.ajalt.clikt.completion.CompletionCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.versionOption
import edu.cornell.cs.apl.viaduct.version

class Viaduct : NoOpCliktCommand(help = "Compile high level specifications to secure distributed programs.") {
    val verbose by verbosity()

    init {
        versionOption(version)
        subcommands(Format(), Compile(), CompletionCommand(), Run(), Evaluate())
        // TODO: Help, Interpret, commands
    }
}
