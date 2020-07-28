package edu.cornell.cs.apl.viaduct.backend

import edu.cornell.cs.apl.viaduct.syntax.intermediate.AtomicExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BlockNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BreakNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.IfNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InfiniteLoopNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SendNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.StatementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode
import edu.cornell.cs.apl.viaduct.syntax.values.BooleanValue
import edu.cornell.cs.apl.viaduct.syntax.values.Value

/** Abstract backend interpreter that interprets control flow statements only. */
abstract class AbstractBackendInterpreter {
    abstract fun pushContext()

    abstract fun popContext()

    abstract fun getContextMarker(): Int

    abstract fun restoreContext(marker: Int)

    abstract suspend fun runAtomicExpr(expr: AtomicExpressionNode): Value

    abstract suspend fun runDeclaration(stmt: DeclarationNode)

    abstract suspend fun runLet(stmt: LetNode)

    abstract suspend fun runUpdate(stmt: UpdateNode)

    abstract suspend fun runSend(stmt: SendNode)

    abstract suspend fun runOutput(stmt: OutputNode)

    suspend fun run(stmt: StatementNode) {
        when (stmt) {
            is DeclarationNode -> runDeclaration(stmt)

            is LetNode -> runLet(stmt)

            is UpdateNode -> runUpdate(stmt)

            is SendNode -> runSend(stmt)

            is OutputNode -> runOutput(stmt)

            is IfNode -> {
                val guardVal = runAtomicExpr(stmt.guard) as BooleanValue

                if (guardVal.value) {
                    run(stmt.thenBranch)
                } else {
                    run(stmt.elseBranch)
                }
            }

            is InfiniteLoopNode -> {
                // communicate loop break by exception
                val contextMarker: Int = getContextMarker()

                try {
                    run(stmt.body)
                    run(stmt)
                } catch (signal: LoopBreakSignal) { // catch loop break signal
                    // this signal is for an outer loop
                    if (signal.jumpLabel != null && signal.jumpLabel != stmt.jumpLabel.value) {
                        throw signal
                    } else { // restore context
                        restoreContext(contextMarker)
                    }
                }
            }

            is BreakNode -> throw LoopBreakSignal(stmt)

            is BlockNode -> {
                pushContext()

                for (childStmt: StatementNode in stmt) {
                    run(childStmt)
                }

                popContext()
            }
        }
    }
}
