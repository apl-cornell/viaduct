package edu.cornell.cs.apl.viaduct.syntax.transformers

import edu.cornell.cs.apl.viaduct.syntax.surface.BlockNode
import edu.cornell.cs.apl.viaduct.syntax.surface.BreakNode
import edu.cornell.cs.apl.viaduct.syntax.surface.ForLoopNode
import edu.cornell.cs.apl.viaduct.syntax.surface.IfNode
import edu.cornell.cs.apl.viaduct.syntax.surface.InfiniteLoopNode
import edu.cornell.cs.apl.viaduct.syntax.surface.StatementNode
import edu.cornell.cs.apl.viaduct.syntax.surface.WhileLoopNode
import kotlinx.collections.immutable.toPersistentList

// TODO: this is broken AF. Doesn't recurse.
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
                    BlockNode(
                        IfNode(
                            stmt.guard,
                            stmt.body,
                            BlockNode(
                                BreakNode(sourceLocation = stmt.sourceLocation),
                                sourceLocation = stmt.sourceLocation
                            ),
                            stmt.sourceLocation
                        ), sourceLocation = stmt.sourceLocation
                    ),
                    null,
                    stmt.sourceLocation
                )
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
                        BlockNode(
                            IfNode(
                                stmt.guard,
                                newBody,
                                BlockNode(
                                    BreakNode(null, stmt.sourceLocation),
                                    sourceLocation = stmt.sourceLocation
                                ),
                                stmt.guard.sourceLocation
                            ), sourceLocation = stmt.sourceLocation
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
