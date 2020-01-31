package edu.cornell.cs.apl.viaduct.passes

import edu.cornell.cs.apl.viaduct.errors.ElaborationException
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariable
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariableNode
import edu.cornell.cs.apl.viaduct.syntax.Temporary
import edu.cornell.cs.apl.viaduct.syntax.TemporaryNode
import edu.cornell.cs.apl.viaduct.util.FreshNameGenerator
import edu.cornell.cs.apl.viaduct.syntax.intermediate.Arguments as IArguments
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AtomicExpressionNode as IAtomicExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BlockNode as IBlockNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BreakNode as IBreakNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode as IDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclassificationNode as IDeclassificationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.EndorsementNode as IEndorsementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExpressionNode as IExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExpressionVisitorOld as IExpressionVisitor
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
import edu.cornell.cs.apl.viaduct.syntax.intermediate.StatementNode as IStatementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode as IUpdateNode
import edu.cornell.cs.apl.viaduct.syntax.surface.AbstractExpressionVisitor as SAbstractExpressionVisitor
import edu.cornell.cs.apl.viaduct.syntax.surface.BlockNode as SBlockNode
import edu.cornell.cs.apl.viaduct.syntax.surface.BreakNode as SBreakNode
import edu.cornell.cs.apl.viaduct.syntax.surface.DeclarationNode as SDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.surface.DeclassificationNode as SDeclassificationNode
import edu.cornell.cs.apl.viaduct.syntax.surface.EndorsementNode as SEndorsementNode
import edu.cornell.cs.apl.viaduct.syntax.surface.ExpressionVisitor as SExpressionVisitor
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
import edu.cornell.cs.apl.viaduct.syntax.surface.StatementVisitor as SStatementVisitor
import edu.cornell.cs.apl.viaduct.syntax.surface.UpdateNode as SUpdateNode
import edu.cornell.cs.apl.viaduct.syntax.surface.VariableContextVisitor as SVariableContextVisitor
import edu.cornell.cs.apl.viaduct.syntax.surface.WhileLoopNode as SWhileLoopNode

/**
 * Convert surface language programs into an intermediate representation
 * that enforces A-normal form syntactically.
 */
class AnfTransformer private constructor() :
    SVariableContextVisitor<AnfTransformer, IExpressionNode, List<IStatementNode>, ObjectVariable>() {

    private val complexExprVisitor = AnfComplexExpressionVisitor()
    private val atomicExprVisitor = AnfAtomicExpressionVisitor()
    private val nameGenerator = FreshNameGenerator()
    private val bindings: MutableList<IStatementNode> = mutableListOf()

    override val exprVisitor: SExpressionVisitor<IExpressionNode>
        get() = complexExprVisitor

    /** bind expression to a let statement. */
    private fun addBinding(bindingFunc: (Temporary) -> IStatementNode): Temporary {
        val tmpName = Temporary(nameGenerator.getFreshName(TMP_NAME))
        val binding = bindingFunc(tmpName)
        bindings.add(binding)
        return tmpName
    }

    private fun withBindings(stmt: IStatementNode): List<IStatementNode> {
        val stmtList = mutableListOf<IStatementNode>()
        stmtList.addAll(bindings)
        stmtList.add(stmt)
        bindings.clear()
        return stmtList
    }

    override fun extract(letNode: SLetNode): ObjectVariable? {
        return null
    }

    override fun extract(declarationNode: SDeclarationNode): ObjectVariable? {
        val newName = nameGenerator.getFreshName(declarationNode.variable.value.name)
        return ObjectVariable(newName)
    }

    override fun leave(stmt: SLetNode, value: IExpressionNode): List<IStatementNode> {
        return withBindings(ILetNode(stmt.temporary, value, stmt.sourceLocation))
    }

    override fun leave(stmt: SDeclarationNode, args: List<IExpressionNode>): List<IStatementNode> {
        val renamedVar = get(stmt.variable.value)
        return withBindings(
            IDeclarationNode(
                ObjectVariableNode(renamedVar, stmt.variable.sourceLocation),
                stmt.label,
                stmt.constructor,
                IArguments(args.map { arg -> atomicExprVisitor.visit(arg) }),
                stmt.sourceLocation
            )
        )
    }

    override fun leave(stmt: SUpdateNode, args: List<IExpressionNode>): List<IStatementNode> {
        val renamedVar = get(stmt.variable.value)

        return withBindings(
            IUpdateNode(
                ObjectVariableNode(renamedVar, stmt.variable.sourceLocation),
                stmt.update,
                IArguments(args.map { arg -> atomicExprVisitor.visit(arg) }),
                stmt.sourceLocation
            )
        )
    }

    override fun leave(
        stmt: SIfNode,
        guard: IExpressionNode,
        thenBranch: List<IStatementNode>,
        elseBranch: List<IStatementNode>
    ): List<IStatementNode> {
        return withBindings(
            IIfNode(
                atomicExprVisitor.visit(guard),
                extractBlock(thenBranch),
                extractBlock(elseBranch),
                stmt.sourceLocation
            )
        )
    }

    override fun leave(stmt: SWhileLoopNode, body: List<IStatementNode>): List<IStatementNode> {
        throw ElaborationException()
    }

    override fun leave(
        stmt: SForLoopNode,
        initialize: List<IStatementNode>,
        update: List<IStatementNode>,
        guard: IExpressionNode,
        body: List<IStatementNode>
    ): List<IStatementNode> {
        throw ElaborationException()
    }

    override fun leave(stmt: SInfiniteLoopNode, body: List<IStatementNode>): List<IStatementNode> {
        return listOf(IInfiniteLoopNode(extractBlock(body), stmt.jumpLabel!!, stmt.sourceLocation))
    }

    override fun leave(stmt: SBreakNode): List<IStatementNode> {
        return listOf(IBreakNode(stmt.jumpLabel!!, stmt.sourceLocation))
    }

    override fun leave(
        stmt: SBlockNode,
        statements: List<List<IStatementNode>>
    ): List<IStatementNode> {
        return listOf(IBlockNode(statements.flatten(), stmt.sourceLocation))
    }

    override fun leave(stmt: SOutputNode, message: IExpressionNode): List<IStatementNode> {
        return withBindings(
            IOutputNode(
                atomicExprVisitor.visit(message),
                stmt.host,
                stmt.sourceLocation
            )
        )
    }

    override fun leave(stmt: SSendNode, message: IExpressionNode): List<IStatementNode> {
        return withBindings(
            ISendNode(
                atomicExprVisitor.visit(message),
                stmt.protocol,
                stmt.sourceLocation
            )
        )
    }

    override fun leave(stmt: SSkipNode): List<IStatementNode> {
        return listOf()
    }

    /** Convert expression's children to atomic expressions. */
    private inner class AnfComplexExpressionVisitor
        : SAbstractExpressionVisitor<AnfComplexExpressionVisitor, IExpressionNode> {
        override fun leave(expr: SLiteralNode): IExpressionNode {
            return ILiteralNode(expr.value, expr.sourceLocation)
        }

        override fun leave(expr: SReadNode): IExpressionNode {
            return IReadNode(expr.temporary, expr.sourceLocation)
        }

        override fun leave(
            expr: SOperatorApplicationNode,
            args: List<IExpressionNode>
        ): IExpressionNode {
            return IOperatorApplicationNode(
                expr.operator,
                IArguments(args.map { arg -> this@AnfTransformer.atomicExprVisitor.visit(arg) }),
                expr.sourceLocation
            )
        }

        override fun leave(expr: SQueryNode, args: List<IExpressionNode>): IExpressionNode {
            val renameVar = this@AnfTransformer.get(expr.variable.value)
            return IQueryNode(
                ObjectVariableNode(renameVar, expr.variable.sourceLocation),
                expr.query,
                IArguments(args.map { arg -> this@AnfTransformer.atomicExprVisitor.visit(arg) }),
                expr.sourceLocation
            )
        }

        override fun leave(
            expr: SDeclassificationNode,
            downgradeExpr: IExpressionNode
        ): IExpressionNode {

            return IDeclassificationNode(
                this@AnfTransformer.atomicExprVisitor.visit(downgradeExpr),
                expr.fromLabel,
                expr.toLabel,
                expr.sourceLocation
            )
        }

        override fun leave(
            expr: SEndorsementNode,
            downgradeExpr: IExpressionNode
        ): IExpressionNode {
            return IEndorsementNode(
                this@AnfTransformer.atomicExprVisitor.visit(downgradeExpr),
                expr.fromLabel,
                expr.toLabel,
                expr.sourceLocation
            )
        }

        override fun leave(expr: SInputNode): IExpressionNode {
            val tmpName = this@AnfTransformer.addBinding { tmp ->
                IInputNode(
                    TemporaryNode(tmp, expr.sourceLocation),
                    expr.type,
                    expr.host,
                    expr.sourceLocation
                )
            }

            return IReadNode(tmpName, expr.sourceLocation)
        }

        override fun leave(expr: SReceiveNode): IExpressionNode {
            val tmpName = this@AnfTransformer.addBinding { tmp ->
                IReceiveNode(
                    TemporaryNode(tmp, expr.sourceLocation),
                    expr.type,
                    expr.protocol,
                    expr.sourceLocation
                )
            }

            return IReadNode(tmpName, expr.sourceLocation)
        }
    }

    /** Add let binding to atomize complex expressions. */
    private inner class AnfAtomicExpressionVisitor : IExpressionVisitor<IAtomicExpressionNode> {
        override fun visit(expr: IExpressionNode): IAtomicExpressionNode {
            return when (expr) {
                is IAtomicExpressionNode -> expr
                else -> {
                    val tmp = this@AnfTransformer.addBinding { tmp ->
                        ILetNode(
                            TemporaryNode(tmp, expr.sourceLocation),
                            expr,
                            expr.sourceLocation
                        )
                    }
                    IReadNode(tmp, expr.sourceLocation)
                }
            }
        }
    }

    companion object : SStatementVisitor<IStatementNode> {
        const val TMP_NAME: String = "TMP"

        private fun extractBlock(stmts: List<IStatementNode>): IBlockNode {
            if (stmts.size == 1) {
                return stmts[0] as IBlockNode
            } else {
                throw IllegalArgumentException("statement list must have a single element")
            }
        }

        override fun visit(stmt: SStatementNode): IStatementNode {
            return extractBlock(AnfTransformer().visit(stmt))
        }
    }
}
