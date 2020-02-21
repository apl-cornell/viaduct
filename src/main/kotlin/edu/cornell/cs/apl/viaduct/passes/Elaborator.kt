package edu.cornell.cs.apl.viaduct.passes

import edu.cornell.cs.apl.viaduct.errorskotlin.JumpOutsideLoopScopeError
import edu.cornell.cs.apl.viaduct.syntax.Arguments
import edu.cornell.cs.apl.viaduct.syntax.JumpLabel
import edu.cornell.cs.apl.viaduct.syntax.JumpLabelNode
import edu.cornell.cs.apl.viaduct.syntax.Located
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariable
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariableNode
import edu.cornell.cs.apl.viaduct.syntax.StatementContext
import edu.cornell.cs.apl.viaduct.syntax.Temporary
import edu.cornell.cs.apl.viaduct.syntax.TemporaryNode
import edu.cornell.cs.apl.viaduct.util.FreshNameGenerator
import java.util.Stack
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AtomicExpressionNode as IAtomicExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BlockNode as IBlockNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BreakNode as IBreakNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode as IDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclassificationNode as IDeclassificationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.EndorsementNode as IEndorsementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExpressionNode as IExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.HostDeclarationNode as IHostDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.IfNode as IIfNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InfiniteLoopNode as IInfiniteLoopNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InputNode as IInputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode as ILetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LiteralNode as ILiteralNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OperatorApplicationNode as IOperatorApplicationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutputNode as IOutputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProcessDeclarationNode as IProcessDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode as IProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.QueryNode as IQueryNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReadNode as IReadNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReceiveNode as IReceiveNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SendNode as ISendNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.StatementNode as IStatementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.TopLevelDeclarationNode as ITopLevelDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode as IUpdateNode
import edu.cornell.cs.apl.viaduct.syntax.surface.AssertionNode as SAssertionNode
import edu.cornell.cs.apl.viaduct.syntax.surface.BlockNode as SBlockNode
import edu.cornell.cs.apl.viaduct.syntax.surface.BreakNode as SBreakNode
import edu.cornell.cs.apl.viaduct.syntax.surface.DeclarationNode as SDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.surface.DeclassificationNode as SDeclassificationNode
import edu.cornell.cs.apl.viaduct.syntax.surface.EndorsementNode as SEndorsementNode
import edu.cornell.cs.apl.viaduct.syntax.surface.ExpressionNode as SExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.surface.ForLoopNode as SForLoopNode
import edu.cornell.cs.apl.viaduct.syntax.surface.HostDeclarationNode as SHostDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.surface.IfNode as SIfNode
import edu.cornell.cs.apl.viaduct.syntax.surface.InfiniteLoopNode as SInfiniteLoopNode
import edu.cornell.cs.apl.viaduct.syntax.surface.InputNode as SInputNode
import edu.cornell.cs.apl.viaduct.syntax.surface.LetNode as SLetNode
import edu.cornell.cs.apl.viaduct.syntax.surface.LiteralNode as SLiteralNode
import edu.cornell.cs.apl.viaduct.syntax.surface.OperatorApplicationNode as SOperatorApplicationNode
import edu.cornell.cs.apl.viaduct.syntax.surface.OutputNode as SOutputNode
import edu.cornell.cs.apl.viaduct.syntax.surface.ProcessDeclarationNode as SProcessDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.surface.ProgramNode as SProgramNode
import edu.cornell.cs.apl.viaduct.syntax.surface.QueryNode as SQueryNode
import edu.cornell.cs.apl.viaduct.syntax.surface.ReadNode as SReadNode
import edu.cornell.cs.apl.viaduct.syntax.surface.ReceiveNode as SReceiveNode
import edu.cornell.cs.apl.viaduct.syntax.surface.SendNode as SSendNode
import edu.cornell.cs.apl.viaduct.syntax.surface.SkipNode as SSkipNode
import edu.cornell.cs.apl.viaduct.syntax.surface.StatementNode as SStatementNode
import edu.cornell.cs.apl.viaduct.syntax.surface.UpdateNode as SUpdateNode
import edu.cornell.cs.apl.viaduct.syntax.surface.WhileLoopNode as SWhileLoopNode

fun SProgramNode.elaborate(): IProgramNode {
    return Elaborator().run(this)
}

/**
 * Elaborates surface programs into intermediate programs by:
 * - Associating each loop and break with a jump label
 * - Desugaring derived forms (while and for loops)
 * - Converting to A-normal form
 * - Renaming all variables to prevent shadowing
 */
private class Elaborator {
    private companion object {
        const val TMP_NAME = "tmp"
        const val LOOP_NAME = "loop"
    }

    private val contextStack = Stack<StatementContext<Temporary, ObjectVariable, JumpLabel>>()
    private val loopStack = Stack<JumpLabel>()
    private val nameGenerator = FreshNameGenerator()
    private val bindings = mutableListOf<IStatementNode>()

    private var context: StatementContext<Temporary, ObjectVariable, JumpLabel>
        get() {
            return contextStack.peek()
        }
        set(value) {
            contextStack.pop()
            contextStack.push(value)
        }

    init {
        contextStack.push(StatementContext())
    }

    /** generic function to add a new binding form. */
    private fun addBinding(bindingFunc: (Temporary) -> IStatementNode): Temporary {
        val tmpName = Temporary(nameGenerator.getFreshName(TMP_NAME))
        val binding = bindingFunc(tmpName)
        bindings.add(binding)
        return tmpName
    }

    /** prepend bindings to statement. */
    private fun withBindings(stmt: IStatementNode): List<IStatementNode> {
        val stmtList = mutableListOf<IStatementNode>()
        stmtList.addAll(bindings)
        stmtList.add(stmt)
        bindings.clear()
        return stmtList
    }

    /** convert into an atomic expression by introducing a new let binding. */
    private fun atomize(expr: IExpressionNode): IAtomicExpressionNode {
        return when (expr) {
            is IAtomicExpressionNode -> expr
            else -> {
                val tmp = addBinding { tmpName ->
                    ILetNode(
                        TemporaryNode(tmpName, expr.sourceLocation),
                        expr,
                        expr.sourceLocation
                    )
                }

                IReadNode(tmp, expr.sourceLocation)
            }
        }
    }

    /** convert surface expression into an intermediate expression. */
    private fun run(expr: SExpressionNode): IExpressionNode {
        return when (expr) {
            is SLiteralNode -> ILiteralNode(expr.value, expr.sourceLocation)

            is SReadNode -> {
                IReadNode(
                    context.get(Located(expr.temporary, expr.sourceLocation)),
                    expr.sourceLocation
                )
            }

            is SOperatorApplicationNode -> {
                IOperatorApplicationNode(
                    expr.operator,
                    Arguments(
                        expr.arguments.map { arg -> atomize(run(arg)) },
                        expr.arguments.sourceLocation
                    ),
                    expr.sourceLocation
                )
            }

            is SQueryNode -> {
                IQueryNode(
                    ObjectVariableNode(
                        context.get(expr.variable),
                        expr.variable.sourceLocation
                    ),
                    expr.query,
                    Arguments(
                        expr.arguments.map { arg -> atomize(run(arg)) },
                        expr.arguments.sourceLocation
                    ),
                    expr.sourceLocation
                )
            }

            is SDeclassificationNode -> {
                IDeclassificationNode(
                    atomize(run(expr.expression)),
                    expr.fromLabel,
                    expr.toLabel,
                    expr.sourceLocation
                )
            }

            is SEndorsementNode -> {
                IEndorsementNode(
                    atomize(run(expr.expression)),
                    expr.fromLabel,
                    expr.toLabel,
                    expr.sourceLocation
                )
            }

            is SInputNode -> {
                val tmp = addBinding { tmpName ->
                    IInputNode(
                        Located(tmpName, expr.sourceLocation),
                        expr.type,
                        expr.host,
                        expr.sourceLocation
                    )
                }

                IReadNode(tmp, expr.sourceLocation)
            }

            is SReceiveNode -> {
                val tmp = addBinding { tmpName ->
                    IReceiveNode(
                        Located(tmpName, expr.sourceLocation),
                        expr.type,
                        expr.protocol,
                        expr.sourceLocation
                    )
                }

                IReadNode(tmp, expr.sourceLocation)
            }
        }
    }

    /** convert surface block into an intermediate block. */
    private fun runBlock(block: SBlockNode): IBlockNode {
        // enter scope
        contextStack.push(context)

        // flatten children into one big list
        val newChildren = mutableListOf<IStatementNode>()
        for (childStmt in block) {
            newChildren.addAll(run(childStmt))
        }

        // leave scope
        contextStack.pop()

        return IBlockNode(newChildren, block.sourceLocation)
    }

    fun run(stmt: SStatementNode): List<IStatementNode> {
        return when (stmt) {
            is SLetNode -> {
                val renamedTmp = Temporary(nameGenerator.getFreshName(stmt.temporary.value.name))
                val result = withBindings(
                    ILetNode(
                        TemporaryNode(renamedTmp, stmt.temporary.sourceLocation),
                        run(stmt.value),
                        stmt.sourceLocation
                    )
                )
                context = context.put(stmt.temporary, renamedTmp)
                result
            }

            is SDeclarationNode -> {
                val renamedObjVar =
                    ObjectVariable(nameGenerator.getFreshName(stmt.variable.value.name))
                val result = withBindings(
                    IDeclarationNode(
                        ObjectVariableNode(renamedObjVar, stmt.variable.sourceLocation),
                        stmt.className,
                        stmt.typeArguments,
                        stmt.labelArguments,
                        Arguments(
                            stmt.arguments.map { arg -> atomize(run(arg)) },
                            stmt.arguments.sourceLocation
                        ),
                        stmt.sourceLocation
                    )
                )
                context = context.put(stmt.variable, renamedObjVar)
                result
            }

            is SUpdateNode -> {
                withBindings(
                    IUpdateNode(
                        ObjectVariableNode(
                            context.get(stmt.variable),
                            stmt.variable.sourceLocation
                        ),
                        stmt.update,
                        Arguments(
                            stmt.arguments.map { arg -> atomize(run(arg)) },
                            stmt.arguments.sourceLocation
                        ),
                        stmt.sourceLocation
                    )
                )
            }

            is SSkipNode -> listOf()

            is SOutputNode -> {
                withBindings(
                    IOutputNode(
                        atomize(run(stmt.message)),
                        stmt.host,
                        stmt.sourceLocation
                    )
                )
            }

            is SSendNode -> {
                withBindings(
                    ISendNode(
                        atomize(run(stmt.message)),
                        stmt.protocol,
                        stmt.sourceLocation
                    )
                )
            }

            is SIfNode -> {
                withBindings(
                    IIfNode(
                        atomize(run(stmt.guard)),
                        runBlock(stmt.thenBranch),
                        runBlock(stmt.elseBranch),
                        stmt.sourceLocation
                    )
                )
            }

            // create jump labels if there aren't any
            is SInfiniteLoopNode -> {
                val jumpLabel = stmt.jumpLabel?.value?.name ?: LOOP_NAME
                val renamedJumpLabel = JumpLabel(nameGenerator.getFreshName(jumpLabel))
                val jumpLabelLocation = stmt.jumpLabel?.sourceLocation ?: stmt.sourceLocation

                // need to push/pop context manually because it won't be treated properly
                // by entering/leaving block
                val oldContext = context
                if (stmt.jumpLabel != null) {
                    context = context.put(stmt.jumpLabel, renamedJumpLabel)
                }

                loopStack.push(renamedJumpLabel)
                val newBody = runBlock(stmt.body)
                loopStack.pop()

                context = oldContext

                listOf(
                    IInfiniteLoopNode(
                        newBody,
                        JumpLabelNode(renamedJumpLabel, jumpLabelLocation),
                        stmt.sourceLocation
                    )
                )
            }

            is SBreakNode -> {
                val jumpLabelNode: JumpLabelNode =
                    if (stmt.jumpLabel == null) {
                        if (!loopStack.empty()) {
                            JumpLabelNode(loopStack.peek(), stmt.sourceLocation)
                        } else {
                            throw JumpOutsideLoopScopeError(stmt)
                        }
                    } else {
                        JumpLabelNode(context.get(stmt.jumpLabel), stmt.jumpLabel.sourceLocation)
                    }

                listOf(
                    IBreakNode(jumpLabelNode, stmt.sourceLocation)
                )
            }

            is SBlockNode -> listOf(runBlock(stmt))

            // desugar into infinite loop node with a conditional inside it
            is SWhileLoopNode ->
                run(elaborate(stmt))

            // desugar into an infinite loop node
            is SForLoopNode ->
                run(elaborate(stmt))

            // TODO: preserve
            is SAssertionNode -> listOf()
        }
    }

    /** convert surface program into intermediate program. */
    fun run(program: SProgramNode): IProgramNode {
        val newTopLevelDecls = mutableListOf<ITopLevelDeclarationNode>()
        for (topLevelDecl in program) {
            val newTopLevelDecl = when (topLevelDecl) {
                is SHostDeclarationNode -> {
                    IHostDeclarationNode(
                        topLevelDecl.name,
                        topLevelDecl.authority,
                        topLevelDecl.sourceLocation
                    )
                }

                is SProcessDeclarationNode -> {
                    IProcessDeclarationNode(
                        topLevelDecl.protocol,
                        run(topLevelDecl.body)[0] as IBlockNode,
                        topLevelDecl.sourceLocation
                    )
                }
            }
            newTopLevelDecls.add(newTopLevelDecl)
        }

        return IProgramNode(newTopLevelDecls, program.sourceLocation)
    }
}

/**
 * Rewrites a while loop into a loop-until-break statement.
 *
 * More specifically,
 *
 * ```
 * while (guard) { body... }
 * ```
 *
 * gets translated to
 *
 * ```
 * loop {
 *     if (guard) {
 *         body...
 *     } else {
 *         break;
 *     }
 * }
 * ```
 */
private fun elaborate(node: SWhileLoopNode): SInfiniteLoopNode =
    SInfiniteLoopNode(
        SBlockNode(
            SIfNode(
                guard = node.guard,
                thenBranch = node.body,
                elseBranch = SBlockNode(
                    SBreakNode(jumpLabel = node.jumpLabel, sourceLocation = node.sourceLocation),
                    sourceLocation = node.sourceLocation
                ),
                sourceLocation = node.sourceLocation
            ),
            sourceLocation = node.sourceLocation
        ),
        jumpLabel = node.jumpLabel,
        sourceLocation = node.sourceLocation
    )

/**
 * Rewrites a for loop into a while loop.
 *
 * More specifically,
 *
 * ```
 * for (init; guard; update) { body... }
 * ```
 *
 * gets translated to
 *
 * ```
 * {
 *     init;
 *     while (guard) { body... update; }
 * }
 * ```
 */
private fun elaborate(node: SForLoopNode): SBlockNode =
    SBlockNode(
        node.initialize,
        SWhileLoopNode(
            guard = node.guard,
            body = SBlockNode(node.body + node.update, sourceLocation = node.sourceLocation),
            jumpLabel = node.jumpLabel,
            sourceLocation = node.sourceLocation
        ),
        sourceLocation = node.sourceLocation
    )
