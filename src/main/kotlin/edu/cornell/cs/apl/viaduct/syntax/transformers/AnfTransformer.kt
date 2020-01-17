package edu.cornell.cs.apl.viaduct.syntax.transformers

import edu.cornell.cs.apl.viaduct.errors.ElaborationException
import edu.cornell.cs.apl.viaduct.syntax.Temporary
import edu.cornell.cs.apl.viaduct.util.FreshNameGenerator
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import edu.cornell.cs.apl.viaduct.syntax.Located as INodeFrom
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AtomicExpressionNode as IAtomicExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BlockNode as IBlockNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BreakNode as IBreakNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode as IDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclassificationNode as IDeclassificationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.EndorsementNode as IEndorsementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExpressionNode as IExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.IfNode as IIfNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InfiniteLoopNode as IInfiniteLoopNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InputNode as IInputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode as ILetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LiteralNode as ILiteralNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OperatorApplicationNode as IOperatorApplicationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutputNode as IOutputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.QueryNode as IQueryNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReadNode as IReadNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReceiveNode as IReceiveNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SendNode as ISendNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SkipNode as ISkipNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.StatementNode as IStatementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode as IUpdateNode
import edu.cornell.cs.apl.viaduct.syntax.surface.BlockNode as SBlockNode
import edu.cornell.cs.apl.viaduct.syntax.surface.BreakNode as SBreakNode
import edu.cornell.cs.apl.viaduct.syntax.surface.DeclarationNode as SDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.surface.DeclassificationNode as SDeclassificationNode
import edu.cornell.cs.apl.viaduct.syntax.surface.EndorsementNode as SEndorsementNode
import edu.cornell.cs.apl.viaduct.syntax.surface.ExpressionNode as SExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.surface.ForLoopNode as SForLoopNode
import edu.cornell.cs.apl.viaduct.syntax.surface.IfNode as SIfNode
import edu.cornell.cs.apl.viaduct.syntax.surface.InfiniteLoopNode as SInfiniteLoopNode
import edu.cornell.cs.apl.viaduct.syntax.surface.InputNode as SInputNode
import edu.cornell.cs.apl.viaduct.syntax.surface.LetNode as SLetNode
import edu.cornell.cs.apl.viaduct.syntax.surface.LiteralNode as SLiteralNode
import edu.cornell.cs.apl.viaduct.syntax.surface.OperatorApplicationNode as SOperatorApplicationNode
import edu.cornell.cs.apl.viaduct.syntax.surface.OutputNode as SOutputNode
import edu.cornell.cs.apl.viaduct.syntax.surface.QueryNode as SQueryNode
import edu.cornell.cs.apl.viaduct.syntax.surface.ReadNode as SReadNode
import edu.cornell.cs.apl.viaduct.syntax.surface.ReceiveNode as SReceiveNode
import edu.cornell.cs.apl.viaduct.syntax.surface.SendNode as SSendNode
import edu.cornell.cs.apl.viaduct.syntax.surface.SkipNode as SSkipNode
import edu.cornell.cs.apl.viaduct.syntax.surface.StatementNode as SStatementNode
import edu.cornell.cs.apl.viaduct.syntax.surface.UpdateNode as SUpdateNode
import edu.cornell.cs.apl.viaduct.syntax.surface.WhileLoopNode as SWhileLoopNode

/**
 * convert surface language programs into an intermediate representation
 * that enforces A-normal form syntactically.
 */
class AnfTransformer {
    companion object {
        const val TMP_NAME: String = "TMP"

        private fun extractBlock(stmts: List<IStatementNode>): IBlockNode {
            if (stmts.size == 1) {
                return stmts[0] as IBlockNode

            } else {
                throw IllegalArgumentException("statement list must have a single element")
            }
        }

        fun run(stmt: SStatementNode): IStatementNode {
            return extractBlock(AnfTransformer().transformStmt(stmt))
        }
    }

    private val freshNameGenerator = FreshNameGenerator()

    /** transform statement into intermediate ANF form.
     *
     * @param stmt the statement to transform.
     */
    private fun transformStmt(stmt: SStatementNode): List<IStatementNode> {
        return when (stmt) {
            is SLetNode -> {
                val stmtList = mutableListOf<IStatementNode>()
                val newExpr = transformAnfExpr(stmt.value, stmtList)
                stmtList.add(ILetNode(stmt.temporary, newExpr, stmt.sourceLocation))
                stmtList
            }

            is SDeclarationNode -> {
                val stmtList = mutableListOf<IStatementNode>()
                val newArgs = stmt.arguments.map { arg -> transformAtomicExpr(arg, stmtList) }
                stmtList.add(
                    IDeclarationNode(
                        stmt.variable,
                        stmt.constructor,
                        newArgs.toImmutableList(),
                        stmt.sourceLocation
                    )
                )
                stmtList
            }

            is SUpdateNode -> {
                val stmtList = mutableListOf<IStatementNode>()
                val newArgs = stmt.arguments.map { arg -> transformAtomicExpr(arg, stmtList) }
                stmtList.add(
                    IUpdateNode(
                        stmt.variable,
                        stmt.update,
                        newArgs.toImmutableList(),
                        stmt.sourceLocation
                    )
                )
                stmtList
            }

            is SSkipNode -> listOf(ISkipNode(stmt.sourceLocation))

            is SIfNode -> {
                val stmtList = mutableListOf<IStatementNode>()
                val newGuard = transformAtomicExpr(stmt.guard, stmtList)
                val newThenBranch = extractBlock(transformStmt(stmt.thenBranch))
                val newElseBranch = extractBlock(transformStmt(stmt.elseBranch))
                stmtList.add(IIfNode(newGuard, newThenBranch, newElseBranch, stmt.sourceLocation))
                stmtList
            }

            is SInfiniteLoopNode -> {
                listOf(
                    IInfiniteLoopNode(
                        extractBlock(transformStmt(stmt.body)),
                        stmt.jumpLabel,
                        stmt.sourceLocation
                    )
                )
            }

            is SBreakNode -> {
                listOf(IBreakNode(stmt.jumpLabel, stmt.sourceLocation))
            }

            is SBlockNode -> {
                val newChildren = mutableListOf<IStatementNode>()
                for (childStmt in stmt.statements) {
                    newChildren.addAll(transformStmt(childStmt))
                }
                listOf(IBlockNode(newChildren.toImmutableList(), stmt.sourceLocation))
            }

            is SOutputNode -> {
                val stmtList = mutableListOf<IStatementNode>()
                val newMessage = transformAtomicExpr(stmt.message, stmtList)
                stmtList.add(IOutputNode(newMessage, stmt.host, stmt.sourceLocation))
                stmtList
            }

            is SSendNode -> {
                val stmtList = mutableListOf<IStatementNode>()
                val newMessage = transformAtomicExpr(stmt.message, stmtList)
                stmtList.add(ISendNode(newMessage, stmt.protocol, stmt.sourceLocation))
                stmtList
            }

            is SWhileLoopNode, is SForLoopNode -> {
                throw ElaborationException()
            }
        }
    }

    /**
     * wrap a fresh name into a temporary.
     */
    private fun getFreshTemporary(): Temporary {
        val varName = freshNameGenerator.getFreshName(TMP_NAME)
        return Temporary(varName)
    }

    /**
     * convert an expression's children into atomic expressions
     * without changing the expression itself
     *
     * @param expr the expression to convert.
     * @param bindings the current list of bindings to update.
     */
    private fun transformAnfExpr(
        expr: SExpressionNode,
        bindings: MutableList<IStatementNode>
    ): IExpressionNode {
        return when (expr) {
            is SReadNode -> {
                IReadNode(expr.temporary, expr.sourceLocation)
            }

            is SLiteralNode -> {
                ILiteralNode(expr.value, expr.sourceLocation)
            }

            is SOperatorApplicationNode -> {
                val newArgs = expr.arguments.map { childExpr -> transformAtomicExpr(childExpr, bindings) }
                IOperatorApplicationNode(
                    expr.operator, newArgs.toImmutableList(), expr.sourceLocation)
            }

            is SQueryNode -> {
                val newArgs = expr.arguments.map { childExpr -> transformAtomicExpr(childExpr, bindings) }
                IQueryNode(
                    expr.variable, expr.query, newArgs.toImmutableList(), expr.sourceLocation)
            }

            is SDeclassificationNode -> {
                val newExpr = transformAtomicExpr(expr.expression, bindings)
                IDeclassificationNode(newExpr, expr.fromLabel, expr.toLabel, expr.sourceLocation)
            }

            is SEndorsementNode -> {
                val newExpr = transformAtomicExpr(expr.expression, bindings)
                IEndorsementNode(newExpr, expr.fromLabel, expr.toLabel, expr.sourceLocation)
            }

            is SInputNode -> {
                val tmp = getFreshTemporary()
                bindings.add(
                    IInputNode(
                        INodeFrom(tmp, expr.sourceLocation), expr.type, expr.host, expr.sourceLocation
                    )
                )
                IReadNode(tmp, expr.sourceLocation)
            }

            is SReceiveNode -> {
                val tmp = getFreshTemporary()
                bindings.add(
                    IReceiveNode(
                        INodeFrom(tmp, expr.sourceLocation), expr.type, expr.protocol, expr.sourceLocation
                    )
                )
                IReadNode(tmp, expr.sourceLocation)
            }
        }

    }

    /**
     * convert an expression into an atomic expression by introducing a let binding
     * that names the expression and replaces its occurrences with a ReadNode.
     *
     * @param expr the expression to convert.
     * @param bindings current list of bindings to update.
     */
    private fun transformAtomicExpr(
        expr: SExpressionNode,
        bindings: MutableList<IStatementNode>
    ): IAtomicExpressionNode {
        return when (val newExpr = transformAnfExpr(expr, bindings)) {
            is IAtomicExpressionNode -> newExpr
            else -> {
                val tmp = getFreshTemporary()
                bindings.add(
                    ILetNode(INodeFrom(tmp, expr.sourceLocation), newExpr, expr.sourceLocation)
                )
                IReadNode(tmp, newExpr.sourceLocation)
            }
        }
    }
}

