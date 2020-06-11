package edu.cornell.cs.apl.viaduct.cli

import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.versionOption
import edu.cornell.cs.apl.viaduct.version

class Viaduct : NoOpCliktCommand(help = "Compile high level specifications to secure distributed programs.") {
    val verbose by verbosity()

    init {
        versionOption(version)
        subcommands(Format(), Compile(), Specification(), Run())
        // TODO: Help, Interpret, Specification commands
    }

    override fun aliases(): Map<String, List<String>> =
        mapOf("spec" to listOf(Specification().commandName))
}
