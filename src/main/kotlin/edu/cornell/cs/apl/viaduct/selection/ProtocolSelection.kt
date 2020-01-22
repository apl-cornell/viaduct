package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.Variable
import edu.cornell.cs.apl.viaduct.syntax.intermediate.StatementNode
import kotlinx.collections.immutable.PersistentMap

typealias ProtocolMap = PersistentMap<Variable, Protocol>

data class ProtocolSelectionContext(
    val hostConfig: HostTrustConfiguration
    // labelMap
    // typeMap
    // metadataMap

    // metadata needed by protocol selection
    // - isArrayIndex
    // - isLoopGuard
    // - isConditionalGuard
    // - isInLoopBody
    // - isConstructorArgument
)

interface ProtocolSelection {
    fun selectProtocols(hostConfig: HostTrustConfiguration, stmt: StatementNode): ProtocolMap?
}
