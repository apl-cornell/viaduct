package edu.cornell.cs.apl.viaduct.selection

interface ProtocolCostEstimator {
    fun estimateCost(protocolMap: ProtocolMap): ProtocolCost
}
