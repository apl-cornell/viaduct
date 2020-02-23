package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.intermediate.StatementNode

/** build metadata describing syntactic information about data and computations,
 *  to be used by protocol selection. */
open class ProtocolSelectionContext(
    val hostConfig: HostTrustConfiguration,
    val stmt: StatementNode
) {
    /*
    init {
        buildStmtMetadata(stmt, persistentListOf())
    }

    /** computations that are in array indices. */
    private val arrayIndices = mutableSetOf<Temporary>()

    /** computations that are in constructor arguments. */
    private val constructorArguments = mutableSetOf<Temporary>()

    /** computations that are in downgrades. */
    private val downgradeExpressions = mutableSetOf<Temporary>()

    /** computations that are conditional guards. */
    private val conditionalGuards = mutableSetOf<Temporary>()

    /** computations that are loop guards. */
    private val loopGuards = mutableSetOf<Temporary>()

    /** parent exprs to child exprs. */
    private val readMap = mutableMapOf<Temporary, Set<Temporary>>()

    /** child exprs to parent exprs. */
    private val parentMap = mutableMapOf<Temporary, Temporary>()

    /** temporaries to the let bindings that define them. */
    private val letBindingMap = mutableMapOf<Temporary, LetNode>()

    /** object vars to their object declarations. */
    private val declarationMap = mutableMapOf<ObjectVariable, DeclarationNode>()

    /** control structures and in which the computation or declaration is defined. */
    private val controlContextMap = mutableMapOf<SimpleStatementNode, List<ControlNode>>()

    private fun updateReadMap(letStmt: LetNode) {
        val tmp = letStmt.temporary.value
        when (val expr = letStmt.value) {
            is ReadNode -> {
                readMap[tmp] = persistentSetOf(expr.temporary)
                parentMap[expr.temporary] = tmp
            }

            is OperatorApplicationNode -> {
                readMap[tmp] = expr.arguments.reads
                for (read in expr.arguments.reads) {
                    parentMap[read] = tmp
                }
            }

            is QueryNode -> {
                readMap[tmp] = expr.arguments.reads
                for (read in expr.arguments.reads) {
                    parentMap[read] = tmp
                }
            }

            is DowngradeNode -> {
                when (val downgradeExpr = expr.expression) {
                    is ReadNode -> {
                        readMap[tmp] = persistentSetOf(downgradeExpr.temporary)
                        parentMap[downgradeExpr.temporary] = tmp
                    }
                }
            }
        }
    }

    private fun updateControlContext(
        stmt: SimpleStatementNode,
        controlContext: PersistentList<ControlNode>
    ) {
        controlContextMap[stmt] = controlContext
    }

    private fun buildStmtMetadata(
        stmt: StatementNode,
        controlContext: PersistentList<ControlNode>
    ) {
        when (stmt) {
            is LetNode -> {
                updateReadMap(stmt)
                updateControlContext(stmt, controlContext)
                letBindingMap[stmt.temporary.value] = stmt

                when (val letStmt = stmt.value) {
                    is QueryNode -> {
                        when (letStmt.query) {
                            is ArrayGet -> {
                                when (val arg = letStmt.arguments[0]) {
                                    is ReadNode -> arrayIndices.add(arg.temporary)
                                }
                            }
                        }
                    }

                    is DowngradeNode -> {
                        when (val expr = stmt.value) {
                            is ReadNode -> downgradeExpressions.add(expr.temporary)
                        }
                    }
                }
            }

            is DeclarationNode -> {
                updateControlContext(stmt, controlContext)
                declarationMap[stmt.variable.value] = stmt
                for (arg in stmt.arguments) {
                    when (arg) {
                        is ReadNode -> constructorArguments.add(arg.temporary)
                    }
                }
            }

            is UpdateNode -> {
                updateControlContext(stmt, controlContext)
            }

            is InputNode -> {
                updateControlContext(stmt, controlContext)
            }

            is OutputNode -> {
                updateControlContext(stmt, controlContext)
            }

            is ReceiveNode -> {
                updateControlContext(stmt, controlContext)
            }

            is SendNode -> {
                updateControlContext(stmt, controlContext)
            }

            is IfNode -> {
                when (val expr = stmt.guard) {
                    is ReadNode -> {
                        if (!loopGuards.contains(expr.temporary)) {
                            conditionalGuards.add(expr.temporary)
                        }
                    }
                }
                val branchControlContext = controlContext.add(stmt)
                buildStmtMetadata(stmt.thenBranch, branchControlContext)
                buildStmtMetadata(stmt.elseBranch, branchControlContext)
            }

            is InfiniteLoopNode -> {
                stmt.body.singletonStatement()?.apply {
                    when (this) {
                        is IfNode -> {
                            when (val guard = this.guard) {
                                is ReadNode -> loopGuards.add(guard.temporary)
                            }
                        }
                    }
                }

                buildStmtMetadata(stmt.body, controlContext.add(stmt))
            }

            is BlockNode -> {
                for (childStmt in stmt.statements) {
                    buildStmtMetadata(childStmt, controlContext)
                }
            }
        }
    }

    /** check if an expression is a subexpression of a larger expr that is part of some set. */
    private fun isSubexpressionInSet(tmp: Temporary, set: Set<Temporary>): Boolean {
        var current: Temporary? = tmp
        while (current != null) {
            if (set.contains(current)) {
                return true
            } else {
                current = parentMap[current]
            }
        }

        return false
    }

    fun inArrayIndex(tmp: Temporary): Boolean {
        return isSubexpressionInSet(tmp, arrayIndices)
    }

    fun inConstructorArgument(tmp: Temporary): Boolean {
        return isSubexpressionInSet(tmp, constructorArguments)
    }

    fun inLoopGuard(tmp: Temporary): Boolean {
        return isSubexpressionInSet(tmp, loopGuards)
    }

    fun inConditionalGuard(tmp: Temporary): Boolean {
        return isSubexpressionInSet(tmp, conditionalGuards)
    }

    fun inLoop(v: Variable): Boolean {
        return when (v) {
            is Temporary -> letBindingMap[v]
            is ObjectVariable -> declarationMap[v]
        }?.let { stmt ->
            controlContextMap[stmt]?.filterIsInstance<InfiniteLoopNode>()?.any()
        } ?: false
    }
    */
}
