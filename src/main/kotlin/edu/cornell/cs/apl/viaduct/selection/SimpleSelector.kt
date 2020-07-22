package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.viaduct.analysis.InformationFlowAnalysis
import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.protocols.ABY
import edu.cornell.cs.apl.viaduct.protocols.Local
import edu.cornell.cs.apl.viaduct.protocols.Replication
import edu.cornell.cs.apl.viaduct.syntax.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.Protocol
// TODO this might duplicate work for HostTrustConfiguration

fun SimpleSelector(nameAnalysis: NameAnalysis, informationFlowAnalysis: InformationFlowAnalysis): ProtocolSelector {
    val hostTrustConfiguration = HostTrustConfiguration(nameAnalysis.tree.root)
    return unions(ABYSelector(nameAnalysis, hostTrustConfiguration, informationFlowAnalysis),
        ReplicationSelector(hostTrustConfiguration, informationFlowAnalysis),
        LocalSelector(hostTrustConfiguration, informationFlowAnalysis))
}

fun simpleProtocolCost(p: Protocol): Int {
    return when (p) {
        is Local -> 0
        is Replication -> 1
        is ABY -> 2
        else -> 10
    }
}
