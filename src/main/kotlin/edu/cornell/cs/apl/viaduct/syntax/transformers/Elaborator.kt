package edu.cornell.cs.apl.viaduct.syntax.transformers

import edu.cornell.cs.apl.viaduct.syntax.surface.*
import kotlinx.collections.immutable.toPersistentList

class Elaborator {
    companion object {
        fun run(stmt: StatementNode): StatementNode {
            return Elaborator().elaborate(stmt)
        }
    }

    /**
     * elaborate derived forms in a statement.
     *
     * @param stmt the statement to elaborate.
     */
    private fun elaborate(stmt: StatementNode): StatementNode {
        return when (stmt) {
            is WhileLoopNode -> {
                InfiniteLoopNode(
                    blockOf(
                        IfNode(
                            stmt.guard,
                            stmt.body,
                            blockOf(BreakNode(null, stmt.sourceLocation)),
                            stmt.sourceLocation)),
                    null,
                    stmt.sourceLocation)
            }

            is ForLoopNode -> {
                val bodyBlock = mutableListOf<StatementNode>()
                bodyBlock.addAll(stmt.body.statements)
                bodyBlock.add(stmt.update)

                val newBody = BlockNode(bodyBlock.toPersistentList(), stmt.body.sourceLocation)

                val block = mutableListOf<StatementNode>()
                block.add(stmt.initialize)
                block.add(
                    InfiniteLoopNode(
                        blockOf(
                            IfNode(
                                stmt.guard,
                                newBody,
                                blockOf(BreakNode(null, stmt.sourceLocation)),
                                stmt.guard.sourceLocation
                            )
                        ),
                        null,
                        stmt.sourceLocation
                    )
                )
                BlockNode(block.toPersistentList(), stmt.sourceLocation)
            }

            else -> stmt
        }
    }
}
