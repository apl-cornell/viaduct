package edu.cornell.cs.apl.viaduct.syntax.intermediate

import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.SourceLocation
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap

/** A program is a sequence of top level declarations. */
class ProgramNode(
    declarations: List<TopLevelDeclarationNode>,
    override val sourceLocation: SourceLocation
) : Node(), List<TopLevelDeclarationNode> by declarations {
    // Make an immutable copy
    val declarations: List<TopLevelDeclarationNode> = declarations.toPersistentList()

    /** Host declarations in the program. */
    val hosts: Map<Host, HostDeclarationNode> by lazy {
        val hosts: MutableMap<Host, HostDeclarationNode> = HashMap()
        declarations.filterIsInstance<HostDeclarationNode>().forEach { declaration ->
            val host = declaration.name.value
            require(hosts[host] == null) { "Duplicate host declaration: $host" }
            hosts[host] = declaration
        }
        hosts.toPersistentMap()
    }

    /** Process declarations in the program. */
    val processes: Map<Protocol, ProcessDeclarationNode> by lazy {
        val processes: MutableMap<Protocol, ProcessDeclarationNode> = HashMap()
        declarations.filterIsInstance<ProcessDeclarationNode>().forEach { declaration ->
            val protocol = declaration.protocol.value
            require(processes[protocol] == null) { "Duplicate process declaration: $protocol" }
            processes[protocol] = declaration
        }
        processes.toPersistentMap()
    }
}
