package edu.cornell.cs.apl.viaduct.lowering

import edu.cornell.cs.apl.viaduct.analysis.AnalysisProvider
import edu.cornell.cs.apl.viaduct.analysis.main
import edu.cornell.cs.apl.viaduct.syntax.JumpLabel
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AssertionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BlockNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BreakNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclassificationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.EndorsementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.FunctionCallNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.IfNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InfiniteLoopNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LiteralNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OperatorApplicationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutParameterInitializationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.QueryNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReadNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SimpleStatementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.StatementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode
import edu.cornell.cs.apl.viaduct.util.FreshNameGenerator
import kotlinx.collections.immutable.toPersistentList
import edu.cornell.cs.apl.viaduct.lowering.DeclarationNode as LDeclarationNode
import edu.cornell.cs.apl.viaduct.lowering.InputNode as LInputNode
import edu.cornell.cs.apl.viaduct.lowering.LetNode as LLetNode
import edu.cornell.cs.apl.viaduct.lowering.LiteralNode as LLiteralNode
import edu.cornell.cs.apl.viaduct.lowering.OperatorApplicationNode as LOperatorApplicationNode
import edu.cornell.cs.apl.viaduct.lowering.OutputNode as LOutputNode
import edu.cornell.cs.apl.viaduct.lowering.QueryNode as LQueryNode
import edu.cornell.cs.apl.viaduct.lowering.ReadNode as LReadNode
import edu.cornell.cs.apl.viaduct.lowering.UpdateNode as LUpdateNode

class LoweringPass private constructor (val block: BlockNode) {
    private val nameGenerator = FreshNameGenerator()
    private val blockMap = mutableMapOf<RegularBlockLabel, LoweredBasicBlock<RegularBlockLabel>>()
    private val breakLabelMap = mutableMapOf<JumpLabel, RegularBlockLabel>()

    private fun freshBlockLabel(): RegularBlockLabel {
        return RegularBlockLabel(nameGenerator.getFreshName("block"))
    }

    private fun lowerExpression(expr: ExpressionNode): LoweredExpression {
        return when (expr) {
            is InputNode -> LInputNode(expr.type.value, expr.host.value)
            is LiteralNode -> LLiteralNode(expr.value)
            is ReadNode -> LReadNode(expr.temporary.value)
            is DeclassificationNode -> lowerExpression(expr.expression)
            is EndorsementNode -> lowerExpression(expr.expression)
            is OperatorApplicationNode -> {
                val loweredArgs = expr.arguments.map { lowerExpression(it) }
                LOperatorApplicationNode(expr.operator, loweredArgs.toPersistentList())
            }
            is QueryNode -> {
                val loweredArgs = expr.arguments.map { lowerExpression(it) }
                LQueryNode(expr.variable.value, expr.query.value, loweredArgs.toPersistentList())
            }
        }
    }

    private fun lowerSimpleStatement(stmt: SimpleStatementNode): LoweredStatement? {
        return when (stmt) {
            is DeclarationNode -> {
                val loweredArgs = stmt.arguments.map { lowerExpression(it) }
                LDeclarationNode(
                    stmt.name.value,
                    stmt.className.value,
                    stmt.typeArguments.map { it.value }.toPersistentList(),
                    loweredArgs.toPersistentList(),
                    stmt.protocol!!.value
                )
            }

            is LetNode ->
                LLetNode(
                    stmt.temporary.value,
                    lowerExpression(stmt.value),
                    stmt.protocol!!.value
                )

            is OutParameterInitializationNode -> null

            is OutputNode ->
                LOutputNode(lowerExpression(stmt.message), stmt.host.value)

            is UpdateNode -> {
                val loweredArgs = stmt.arguments.map { lowerExpression(it) }
                LUpdateNode(
                    stmt.variable.value,
                    stmt.update.value,
                    loweredArgs.toPersistentList()
                )
            }
        }
    }

    private fun lower(
        currentLabel: RegularBlockLabel,
        currentBlock: MutableList<LoweredStatement>,
        stmt: StatementNode
    ): Pair<RegularBlockLabel, MutableList<LoweredStatement>> {
        return when (stmt) {
            // TODO: make these no-ops for now
            is AssertionNode -> Pair(currentLabel, currentBlock)
            is FunctionCallNode -> Pair(currentLabel, currentBlock)
            is OutParameterInitializationNode -> Pair(currentLabel, currentBlock)

            is SimpleStatementNode ->
                lowerSimpleStatement(stmt)?.let { loweredStmt ->
                    currentBlock.add(loweredStmt)
                    Pair(currentLabel, currentBlock)
                } ?: Pair(currentLabel, currentBlock)

            is BlockNode -> {
                var lastLabel = currentLabel
                var lastBlock = currentBlock
                for (blockStmt in stmt.statements) {
                    val (outLabel, outBlock) = lower(lastLabel, lastBlock, blockStmt)
                    lastLabel = outLabel
                    lastBlock = outBlock
                }

                Pair(lastLabel, lastBlock)
            }

            is BreakNode -> {
                val gotoLabel = breakLabelMap[stmt.jumpLabel.value]!!
                blockMap[currentLabel] = LoweredBasicBlock(currentBlock, Goto(gotoLabel))
                return Pair(freshBlockLabel(), mutableListOf())
            }

            is IfNode -> {
                val loweredGuard = lowerExpression(stmt.guard)
                val thenLabel = freshBlockLabel()
                val elseLabel = freshBlockLabel()
                val joinLabel = freshBlockLabel()

                val ifJump = GotoIf(loweredGuard, thenLabel, elseLabel)
                blockMap[currentLabel] = LoweredBasicBlock(currentBlock, ifJump)

                val (lastThenLabel, lastThenBlock) = lower(thenLabel, mutableListOf(), stmt.thenBranch)
                blockMap[lastThenLabel] = LoweredBasicBlock(lastThenBlock, Goto(joinLabel))

                val (lastElseLabel, lastElseBlock) = lower(elseLabel, mutableListOf(), stmt.elseBranch)
                blockMap[lastElseLabel] = LoweredBasicBlock(lastElseBlock, Goto(joinLabel))

                Pair(joinLabel, mutableListOf())
            }

            is InfiniteLoopNode -> {
                val startLabel = freshBlockLabel()
                val exitLabel = freshBlockLabel()

                blockMap[currentLabel] = LoweredBasicBlock(currentBlock, Goto(startLabel))
                breakLabelMap[stmt.jumpLabel.value] = exitLabel

                val (lastBodyLabel, lastBodyBlock) = lower(startLabel, mutableListOf(), stmt.body)
                breakLabelMap.remove(stmt.jumpLabel.value)
                blockMap[lastBodyLabel] = LoweredBasicBlock(lastBodyBlock, Goto(startLabel))

                Pair(exitLabel, mutableListOf())
            }
        }
    }

    val flowchartProgram: FlowchartProgram by lazy {
        val (lastLabel, lastBlock) = lower(ENTRY_POINT_LABEL, mutableListOf(), block)
        blockMap[lastLabel] = LoweredBasicBlock(lastBlock, RegularHalt)
        FlowchartProgram(blockMap)
    }

    companion object : AnalysisProvider<LoweringPass> {
        private fun construct(program: ProgramNode) = LoweringPass(program.main.body)

        override fun get(program: ProgramNode): LoweringPass = program.cached(::construct)
    }
}
