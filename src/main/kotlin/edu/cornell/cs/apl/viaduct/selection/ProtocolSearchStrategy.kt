package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.Variable
import kotlinx.collections.immutable.PersistentSet

interface ProtocolSearchStrategy {
    fun createProtocolInstances(
        hostConfig: HostTrustConfiguration,
        currentProtoMap: ProtocolMap,
        nextVariable: Variable
    ): PersistentSet<ProtocolMap>
}
