package io.github.apl_cornell.viaduct.cli

import com.github.ajalt.clikt.completion.CompletionCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.versionOption
import io.github.apl_cornell.viaduct.version

class Viaduct : NoOpCliktCommand(help = "Compile high level specifications to secure distributed programs.") {
    val verbose by verbosity()

    init {
        versionOption(version)
        subcommands(Format(), Compile(), CompletionCommand(), Run())
        // TODO: Help, Interpret, commands
    }
}
