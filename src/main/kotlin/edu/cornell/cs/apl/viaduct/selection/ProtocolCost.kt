package edu.cornell.cs.apl.viaduct.selection

sealed class ProtocolCost

data class ValidProtocolCost(val cost: Int) : ProtocolCost()

object InvalidProtocolCost : ProtocolCost()
