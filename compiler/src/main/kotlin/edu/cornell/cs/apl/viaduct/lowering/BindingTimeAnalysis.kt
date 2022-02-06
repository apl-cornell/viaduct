package edu.cornell.cs.apl.viaduct.lowering

import edu.cornell.cs.apl.viaduct.algebra.MeetSemiLattice
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariable
import edu.cornell.cs.apl.viaduct.syntax.Temporary
import edu.cornell.cs.apl.viaduct.util.FreshNameGenerator
import edu.cornell.cs.apl.viaduct.util.dataflow.DataFlowNode
import edu.cornell.cs.apl.viaduct.util.dataflow.IdentityEdge
import edu.cornell.cs.apl.viaduct.util.dataflow.solveDataFlow
import org.jgrapht.graph.DefaultDirectedGraph

enum class BindingTime : MeetSemiLattice<BindingTime> {
    STATIC, DYNAMIC;

    override fun meet(that: BindingTime): BindingTime =
        if (this == DYNAMIC || that == DYNAMIC) DYNAMIC else STATIC

    override fun lessThanOrEqualTo(that: BindingTime): Boolean =
        this == DYNAMIC || that == STATIC
}

sealed class BindingTimeAnalysisNode : DataFlowNode<BindingTime>

data class ObjectBindingTimeNode(val variable: ObjectVariable) : BindingTimeAnalysisNode() {
    override fun transfer(input: BindingTime): BindingTime = input
}

data class TemporaryBindingTimeNode(val temporary: Temporary) : BindingTimeAnalysisNode() {
    override fun transfer(input: BindingTime): BindingTime = input
}

data class BlockBindingTimeNode(val block: RegularBlockLabel) : BindingTimeAnalysisNode() {
    override fun transfer(input: BindingTime): BindingTime = input
}

data class ConstantBindingTimeNode(val value: BindingTime) : BindingTimeAnalysisNode() {
    override fun transfer(input: BindingTime): BindingTime = value
}

data class VariableBindingTimeNode(val name: String) : BindingTimeAnalysisNode() {
    override fun transfer(input: BindingTime): BindingTime = input
}

class BindingTimeAnalysis private constructor(val program: FlowchartProgram) {
    companion object {
        fun computeBindingTime(program: FlowchartProgram): Map<ObjectVariable, BindingTime> {
            return BindingTimeAnalysis(program).computeObjectBindingTime()
        }
    }

    private val graph = DefaultDirectedGraph<BindingTimeAnalysisNode, IdentityEdge<BindingTime>>(null, null, false)
    private val nameGenerator = FreshNameGenerator()

    private val objectNodes = mutableMapOf<ObjectVariable, ObjectBindingTimeNode>()
    private val temporaryNodes = mutableMapOf<Temporary, TemporaryBindingTimeNode>()
    private val blockNodes = mutableMapOf<RegularBlockLabel, BlockBindingTimeNode>()

    private fun objectNode(variable: ObjectVariable): ObjectBindingTimeNode =
        objectNodes.getOrPut(variable) { ObjectBindingTimeNode(variable) }

    private fun temporaryNode(temporary: Temporary): TemporaryBindingTimeNode =
        temporaryNodes.getOrPut(temporary) { TemporaryBindingTimeNode(temporary) }

    private fun blockNode(label: RegularBlockLabel): BlockBindingTimeNode =
        blockNodes.getOrPut(label) { BlockBindingTimeNode(label) }

    private fun getFreshVariable(): VariableBindingTimeNode {
        return VariableBindingTimeNode(nameGenerator.getFreshName("var"))
    }

    private fun flowsTo(lhs: BindingTimeAnalysisNode, rhs: BindingTimeAnalysisNode) {
        graph.addVertex(lhs)
        graph.addVertex(rhs)
        graph.addEdge(lhs, rhs, IdentityEdge())
    }

    private fun generateBindingTimeConstraints(expr: LoweredExpression): BindingTimeAnalysisNode =
        when (expr) {
            is InputNode -> ConstantBindingTimeNode(BindingTime.DYNAMIC)

            is LiteralNode -> ConstantBindingTimeNode(BindingTime.STATIC)

            is OperatorApplicationNode -> {
                val node = getFreshVariable()

                expr.arguments.map {
                    generateBindingTimeConstraints(it)
                }.forEach { argNode -> flowsTo(argNode, node) }

                node
            }

            is QueryNode -> {
                val node = getFreshVariable()

                flowsTo(objectNode(expr.variable), node)
                expr.arguments.map {
                    generateBindingTimeConstraints(it)
                }.forEach { argNode -> flowsTo(argNode, node) }

                node
            }

            is ReadNode -> temporaryNode(expr.temporary)
        }

    private fun generateBindingTimeConstraints(block: RegularBlockLabel, stmt: LoweredStatement) {
        when (stmt) {
            is DeclarationNode -> {
                val node = objectNode(stmt.name)

                stmt.arguments.map {
                    generateBindingTimeConstraints(it)
                }.forEach { argNode -> flowsTo(argNode, node) }

                // flowsTo(blockNode(block), node)
            }

            is LetNode -> {
                val node = temporaryNode(stmt.temporary)
                val valueNode = generateBindingTimeConstraints(stmt.value)
                flowsTo(valueNode, node)
            }

            is OutputNode -> {}

            SkipNode -> {}

            is UpdateNode -> {
                val node = objectNode(stmt.variable)
                stmt.arguments.map {
                    generateBindingTimeConstraints(it)
                }.forEach { argNode -> flowsTo(argNode, node) }

                flowsTo(blockNode(block), node)
            }
        }
    }

    private fun generateBindingTimeConstraints(
        transitiveSuccessors: Set<RegularBlockLabel>,
        postdominators: Set<RegularBlockLabel>,
        label: RegularBlockLabel,
        block: LoweredBasicBlock<RegularBlockLabel>
    ) {
        // generate constraints from statements
        for (stmt in block.statements) {
            generateBindingTimeConstraints(label, stmt)
        }

        // add PC label constraints: if this block has a conditional jump,
        // there is an implicit flow from the guard's label to
        // the PC labels of transitive successor blocks that are *not* postdominators
        when (val jump = block.jump) {
            is GotoIf -> {
                val guardNode = generateBindingTimeConstraints(jump.guard)
                for (successor in transitiveSuccessors) {
                    if (!postdominators.contains(successor)) {
                        flowsTo(guardNode, blockNode(successor))
                    }
                }
            }

            else -> {}
        }
    }

    fun computeObjectBindingTime(): Map<ObjectVariable, BindingTime> {
        val postdominatorMap = program.computePostdominators()
        val transitiveSuccessorMap = program.computeTransitiveSuccessors()

        for (kv in program.blocks) {
            generateBindingTimeConstraints(
                postdominatorMap[kv.key]!!,
                transitiveSuccessorMap[kv.key]!!,
                kv.key,
                kv.value
            )
        }

        val solution = solveDataFlow(BindingTime.STATIC, graph)
        return objectNodes.map { kv -> kv.key to solution[kv.value]!! }.toMap()
    }
}
