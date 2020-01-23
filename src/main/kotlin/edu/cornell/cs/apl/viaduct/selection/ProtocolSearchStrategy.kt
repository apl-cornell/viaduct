package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.viaduct.syntax.Variable
import kotlinx.collections.immutable.PersistentSet

interface ProtocolSearchStrategy {
    fun createProtocolInstances(
        context: ProtocolSelectionContext,
        currentProtoMap: ProtocolMap,
        nextVariable: Variable
    ): PersistentSet<ProtocolMap>
}
