package edu.cornell.cs.apl.viaduct.backend

import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BlockNode

interface CppBackend {
    val supportedProtocols: Set<String>

    fun compile(block: BlockNode, protocol: Protocol, host: Host): CppBlock
}
