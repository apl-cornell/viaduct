package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.Variable
import edu.cornell.cs.apl.viaduct.syntax.intermediate.StatementNode
import edu.cornell.cs.apl.viaduct.syntax.util.VariableExtractor

import kotlinx.collections.immutable.persistentMapOf
import java.util.PriorityQueue

private data class ProtocolSearchNode(
    val protocolMap: ProtocolMap,
    val cost: Int
) : Comparable<ProtocolSearchNode> {
    override fun compareTo(other: ProtocolSearchNode): Int {
        return this.cost - other.cost
    }
}

class ProtocolSearchSelection(
    val searchStrategy: ProtocolSearchStrategy,
    val costEstimator: ProtocolCostEstimator
): ProtocolSelection {

    override fun selectProtocols(
        context: ProtocolSelectionContext,
        stmt: StatementNode
    ): ProtocolMap? {
        val initProtocolMap = persistentMapOf<Variable, Protocol>()
        val variableList = VariableExtractor.run(stmt)

        val openSet = PriorityQueue<ProtocolSearchNode>()
        openSet.add(ProtocolSearchNode(initProtocolMap, 0))

        val closedSet = mutableSetOf<ProtocolSearchNode>()

        var lastAddedProtocolMap: ProtocolMap = initProtocolMap
        while (!openSet.isEmpty()) {
            val currentSearchNode: ProtocolSearchNode = openSet.remove()
            val currentProtoMap = currentSearchNode.protocolMap

            if (currentProtoMap.size == variableList.size) {
                return currentProtoMap
            }

            val nextVariable = variableList[currentProtoMap.size]
            val protocolMapNeighbors =
                searchStrategy.createProtocolInstances(context, currentProtoMap, nextVariable)

            for (neighbor in protocolMapNeighbors) {
                when (val estimate = costEstimator.estimateCost(neighbor)) {
                    is ValidProtocolCost -> {
                        val neighborNode = ProtocolSearchNode(neighbor, estimate.cost)
                        if (!closedSet.contains(neighborNode) && !openSet.contains(neighborNode)) {
                            openSet.add(neighborNode)
                            lastAddedProtocolMap = neighborNode.protocolMap
                        }
                    }

                    is InvalidProtocolCost -> {}
                }
            }
        }

        return lastAddedProtocolMap
    }
}
