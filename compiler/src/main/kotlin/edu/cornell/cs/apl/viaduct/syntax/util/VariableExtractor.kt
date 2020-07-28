package edu.cornell.cs.apl.viaduct.syntax.util

import edu.cornell.cs.apl.viaduct.syntax.Variable
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BlockNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.IfNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InfiniteLoopNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.StatementNode
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

// TODO: remove. There is a way to implement ANF transformer without this.
/** extracts variables from statements while preserving program order. */
class VariableExtractor private constructor() {
    // TODO: also, why not use top level functions?
    companion object {
        fun run(stmt: StatementNode): PersistentList<Variable> {
            val variableList = mutableListOf<Variable>()
            extractVariables(stmt, variableList)
            return variableList.toPersistentList()
        }

        private fun extractVariables(stmt: StatementNode, variableList: MutableList<Variable>) {
            when (stmt) {
                is LetNode -> {
                    variableList.add(stmt.temporary.value)
                }

                is DeclarationNode -> {
                    variableList.add(stmt.variable.value)
                }

                is IfNode -> {
                    extractVariables(stmt.thenBranch, variableList)
                    extractVariables(stmt.elseBranch, variableList)
                }

                is InfiniteLoopNode -> {
                    extractVariables(stmt.body, variableList)
                }

                is BlockNode -> {
                    for (childStmt in stmt.statements) {
                        extractVariables(childStmt, variableList)
                    }
                }

                else -> {
                }
            }
        }
    }
}
