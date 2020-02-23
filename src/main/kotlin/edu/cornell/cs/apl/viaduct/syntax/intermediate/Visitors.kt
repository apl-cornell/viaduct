package edu.cornell.cs.apl.viaduct.syntax.intermediate

import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.JumpLabel
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariable
import edu.cornell.cs.apl.viaduct.syntax.ProgramContext
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.StatementContext
import edu.cornell.cs.apl.viaduct.syntax.Temporary

/**
 * A suspended traversal of a [StatementNode]. Allows the node to be traversed 0 or more times.
 *
 * Note that there is no memoization, and the node will be traversed each time the function is
 * invoked. This is done deliberately to allow traversing the node multiple times.
 */
typealias SuspendedTraversal<StatementResult, TemporaryData, ObjectData, LoopData, HostData, ProtocolData> =
        (StatementVisitorWithContext<*, StatementResult, TemporaryData, ObjectData, LoopData, HostData, ProtocolData>) -> StatementResult

/**
 * An expression visitor that uses context information.
 *
 * @param ExpressionResult Data returned from each [ExpressionNode].
 * @param TemporaryData Context information attached to each [Temporary] declaration.
 * @param ObjectData Context information attached to each [ObjectVariable] declaration.
 */
interface ExpressionVisitorWithContext<ExpressionResult, TemporaryData, ObjectData> {
    fun leave(node: LiteralNode): ExpressionResult

    fun leave(node: ReadNode, data: TemporaryData): ExpressionResult

    fun leave(node: OperatorApplicationNode, arguments: List<ExpressionResult>): ExpressionResult

    fun leave(
        node: QueryNode,
        arguments: List<ExpressionResult>,
        data: ObjectData
    ): ExpressionResult

    fun leave(node: DeclassificationNode, expression: ExpressionResult): ExpressionResult

    fun leave(node: EndorsementNode, expression: ExpressionResult): ExpressionResult
}

/**
 * A statement visitor that uses context information.
 *
 * This visitor allows some control over the traversal logic for control nodes.
 * @see [SuspendedTraversal].
 *
 * @param ExpressionResult Data returned from each [ExpressionNode].
 * @param StatementResult Data returned from each [StatementNode].
 * @param TemporaryData Context information attached to each [Temporary] declaration.
 * @param ObjectData Context information attached to each [ObjectVariable] declaration.
 * @param LoopData Context information attached to each [JumpLabel].
 * @param HostData Context information attached to each [Host] declaration.
 * @param ProtocolData Context information attached to each [Protocol] declaration.
 */
interface StatementVisitorWithContext<ExpressionResult, StatementResult, TemporaryData, ObjectData, LoopData, HostData, ProtocolData>
    : ExpressionVisitorWithContext<ExpressionResult, TemporaryData, ObjectData> {
    /**
     * Returns the data that will be associated with the [Temporary] declared by [node].
     *
     * Guaranteed to be called immediately after [leave], and exactly once per call to [leave].
     */
    fun getData(node: LetNode, value: ExpressionResult): TemporaryData

    /**
     * Returns the data that will be associated with the [ObjectVariable] declared by [node].
     *
     * Guaranteed to be called immediately after [leave], and exactly once per call to [leave].
     */
    fun getData(node: DeclarationNode, arguments: List<ExpressionResult>): ObjectData

    /**
     * Returns the data that will be associated with the [JumpLabel] of [node].
     *
     * Guaranteed to be called immediately before [leave], and exactly once per call to [leave].
     */
    fun getData(node: InfiniteLoopNode): LoopData

    /**
     * Returns the data that will be associated with the [Temporary] declared by [node].
     *
     * Guaranteed to be called immediately after [leave], and exactly once per call to [leave].
     */
    fun getData(node: InputNode, data: HostData): TemporaryData

    /**
     * Returns the data that will be associated with the [Temporary] declared by [node].
     *
     * Guaranteed to be called immediately after [leave], and exactly once per call to [leave].
     */
    fun getData(node: ReceiveNode, data: ProtocolData): TemporaryData

    fun leave(node: LetNode, value: ExpressionResult): StatementResult

    fun leave(node: DeclarationNode, arguments: List<ExpressionResult>): StatementResult

    fun leave(
        node: UpdateNode,
        arguments: List<ExpressionResult>,
        data: ObjectData
    ): StatementResult

    fun leave(
        node: IfNode,
        guard: ExpressionResult,
        thenBranch: SuspendedTraversal<StatementResult, TemporaryData, ObjectData, LoopData, HostData, ProtocolData>,
        elseBranch: SuspendedTraversal<StatementResult, TemporaryData, ObjectData, LoopData, HostData, ProtocolData>
    ): StatementResult

    fun leave(
        node: InfiniteLoopNode,
        body: SuspendedTraversal<StatementResult, TemporaryData, ObjectData, LoopData, HostData, ProtocolData>,
        data: LoopData
    ): StatementResult

    fun leave(node: BreakNode, data: LoopData): StatementResult

    fun leave(node: BlockNode, statements: List<StatementResult>): StatementResult

    fun leave(node: InputNode, data: HostData): StatementResult

    fun leave(node: OutputNode, message: ExpressionResult, data: HostData): StatementResult

    fun leave(node: ReceiveNode, data: ProtocolData): StatementResult

    fun leave(node: SendNode, message: ExpressionResult, data: ProtocolData): StatementResult
}

/**
 * A program visitor that uses context information.
 *
 * @param StatementResult Data returned from each [StatementNode].
 * @param DeclarationResult Data returned from each [TopLevelDeclarationNode].
 * @param ProgramResult Data returned from the [ProgramNode].
 * @param HostData Context information attached to each [Host] declaration.
 * @param ProtocolData Context information attached to each [Protocol] declaration.
 */
interface ProgramVisitorWithContext<StatementResult, DeclarationResult, ProgramResult, HostData, ProtocolData> {
    /**
     * Returns the data that will be associated with the [Host] declared by [node].
     *
     * Will be called exactly once before _all_ [leave] methods.
     */
    fun getData(node: HostDeclarationNode): HostData

    /**
     * Returns the data that will be associated with the [Protocol] declared by [node].
     *
     * Will be called exactly once before _all_ [leave] methods.
     */
    fun getData(node: ProcessDeclarationNode): ProtocolData

    fun leave(node: HostDeclarationNode): DeclarationResult

    fun leave(
        node: ProcessDeclarationNode,
        body: SuspendedTraversal<StatementResult, *, *, *, HostData, ProtocolData>
    ): DeclarationResult

    fun leave(node: ProgramNode, declarations: List<DeclarationResult>): ProgramResult
}

/**
 * An expression visitor that does not use context information.
 *
 * @see ExpressionVisitorWithContext
 */
interface ExpressionVisitor<ExpressionResult> :
    ExpressionVisitorWithContext<ExpressionResult, Unit, Unit> {
    override fun leave(node: ReadNode, data: Unit): ExpressionResult {
        return leave(node)
    }

    override fun leave(
        node: QueryNode,
        arguments: List<ExpressionResult>,
        data: Unit
    ): ExpressionResult {
        return leave(node, arguments)
    }

    fun leave(node: ReadNode): ExpressionResult

    fun leave(node: QueryNode, arguments: List<ExpressionResult>): ExpressionResult
}

/**
 * A statement visitor that uses statement level context information (variables and loops),
 * but no top level context information (hosts and protocols).
 *
 * @see StatementVisitorWithContext
 */
interface StatementVisitorWithVariableLoopContext<ExpressionResult, StatementResult, TemporaryData, ObjectData, LoopData> :
    StatementVisitorWithContext<ExpressionResult, StatementResult, TemporaryData, ObjectData, LoopData, Unit, Unit> {
    override fun getData(node: InputNode, data: Unit): TemporaryData {
        return getData(node)
    }

    override fun getData(node: ReceiveNode, data: Unit): TemporaryData {
        return getData(node)
    }

    override fun leave(node: InputNode, data: Unit): StatementResult {
        return leave(node)
    }

    override fun leave(node: OutputNode, message: ExpressionResult, data: Unit): StatementResult {
        return leave(node, message)
    }

    override fun leave(node: ReceiveNode, data: Unit): StatementResult {
        return leave(node)
    }

    override fun leave(node: SendNode, message: ExpressionResult, data: Unit): StatementResult {
        return leave(node, message)
    }

    fun getData(node: InputNode): TemporaryData

    fun getData(node: ReceiveNode): TemporaryData

    fun leave(node: InputNode): StatementResult

    fun leave(node: OutputNode, message: ExpressionResult): StatementResult

    fun leave(node: ReceiveNode): StatementResult

    fun leave(node: SendNode, message: ExpressionResult): StatementResult
}

/**
 * A statement visitor that uses context information for variables only.
 *
 * @see StatementVisitorWithContext
 */
interface StatementVisitorWithVariableContext<ExpressionResult, StatementResult, TemporaryData, ObjectData> :
    StatementVisitorWithVariableLoopContext<ExpressionResult, StatementResult, TemporaryData, ObjectData, Unit> {
    override fun getData(node: InfiniteLoopNode) {}

    override fun leave(
        node: InfiniteLoopNode,
        body: SuspendedTraversal<StatementResult, TemporaryData, ObjectData, Unit, Unit, Unit>,
        data: Unit
    ): StatementResult {
        return leave(node, body)
    }

    override fun leave(node: BreakNode, data: Unit): StatementResult {
        return leave(node)
    }

    fun leave(
        node: InfiniteLoopNode,
        body: SuspendedTraversal<StatementResult, TemporaryData, ObjectData, Unit, Unit, Unit>
    ): StatementResult

    fun leave(node: BreakNode): StatementResult
}

/**
 * A statement visitor that does not use context information.
 *
 * @see StatementVisitorWithContext
 */
interface StatementVisitor<ExpressionResult, StatementResult> :
    ExpressionVisitor<ExpressionResult>,
    StatementVisitorWithVariableContext<ExpressionResult, StatementResult, Unit, Unit> {
    override fun getData(node: LetNode, value: ExpressionResult) {}

    override fun getData(node: DeclarationNode, arguments: List<ExpressionResult>) {}

    override fun leave(
        node: UpdateNode,
        arguments: List<ExpressionResult>,
        data: Unit
    ): StatementResult {
        return leave(node, arguments)
    }

    override fun leave(
        node: IfNode,
        guard: ExpressionResult,
        thenBranch: SuspendedTraversal<StatementResult, Unit, Unit, Unit, Unit, Unit>,
        elseBranch: SuspendedTraversal<StatementResult, Unit, Unit, Unit, Unit, Unit>
    ): StatementResult {
        return leave(node, guard, thenBranch(this), elseBranch(this))
    }

    override fun leave(
        node: InfiniteLoopNode,
        body: SuspendedTraversal<StatementResult, Unit, Unit, Unit, Unit, Unit>
    ): StatementResult {
        return leave(node, body(this))
    }

    fun leave(
        node: IfNode,
        guard: ExpressionResult,
        thenBranch: StatementResult,
        elseBranch: StatementResult
    ): StatementResult

    fun leave(node: InfiniteLoopNode, body: StatementResult): StatementResult

    fun leave(node: UpdateNode, arguments: List<ExpressionResult>): StatementResult
}

/**
 * A program visitor that does not use context information.
 *
 * @see ProgramVisitorWithContext
 */
interface ProgramVisitor<StatementResult, DeclarationResult, ProgramResult> :
    ProgramVisitorWithContext<StatementResult, DeclarationResult, ProgramResult, Unit, Unit> {
    override fun getData(node: HostDeclarationNode) {}

    override fun getData(node: ProcessDeclarationNode) {}
}

/**
 * Traverses the expression's abstract syntax tree in depth-first order producing
 * a result using [visitor].
 */
private fun <ExpressionResult, TemporaryData, ObjectData> ExpressionNode.traverse(
    visitor: ExpressionVisitorWithContext<ExpressionResult, TemporaryData, ObjectData>,
    context: StatementContext<TemporaryData, ObjectData, *>
): ExpressionResult {
    return when (this) {
        is LiteralNode ->
            visitor.leave(this)

        is ReadNode ->
            visitor.leave(this, context.get(temporary))

        is OperatorApplicationNode ->
            visitor.leave(this, arguments.map { it.traverse(visitor, context) })

        is QueryNode ->
            visitor.leave(
                this,
                arguments.map { it.traverse(visitor, context) },
                context.get(this.variable)
            )

        is DeclassificationNode ->
            visitor.leave(this, expression.traverse(visitor, context))

        is EndorsementNode ->
            visitor.leave(this, expression.traverse(visitor, context))
    }
}

/**
 * Traverses the statement's abstract syntax tree in depth-first order producing
 * a result using [visitor].
 */
private fun <ExpressionResult, StatementResult, TemporaryData, ObjectData, LoopData, HostData, ProtocolData> BlockNode.traverse(
    visitor: StatementVisitorWithContext<ExpressionResult, StatementResult, TemporaryData, ObjectData, LoopData, HostData, ProtocolData>,
    initialContext: StatementContext<TemporaryData, ObjectData, LoopData>,
    programContext: ProgramContext<HostData, ProtocolData>
): StatementResult {
    var context = initialContext

    val statements: List<StatementResult> = this.statements.map {
        when (it) {
            is LetNode -> {
                val value = it.value.traverse(visitor, context)
                val result = visitor.leave(it, value)

                // Update context
                val data = visitor.getData(it, value)
                context = context.put(it.temporary, data)

                result
            }

            is DeclarationNode -> {
                val arguments = it.arguments.map { arg -> arg.traverse(visitor, context) }
                val result = visitor.leave(it, arguments)

                // Update context
                val data = visitor.getData(it, arguments)
                context = context.put(it.variable, data)

                result
            }

            is UpdateNode -> {
                val arguments = it.arguments.map { arg -> arg.traverse(visitor, context) }
                visitor.leave(it, arguments, context.get(it.variable))
            }

            is IfNode -> {
                val guard = it.guard.traverse(visitor, context)
                // Kotlin differentiates between a captured var and a val.
                // Copying into a val ensures changes to [context] will not affect the context
                // passes to the closures.
                val contextHere = context
                visitor.leave(
                    it,
                    guard,
                    { visitor -> it.thenBranch.traverse(visitor, contextHere, programContext) },
                    { visitor -> it.elseBranch.traverse(visitor, contextHere, programContext) })
            }

            is InfiniteLoopNode -> {
                val data = visitor.getData(it)
                val contextInBody = context.put(it.jumpLabel, data)
                visitor.leave(
                    it,
                    { visitor -> it.body.traverse(visitor, contextInBody, programContext) },
                    data
                )
            }

            is BreakNode -> {
                visitor.leave(it, context.get(it.jumpLabel))
            }

            is BlockNode -> {
                it.traverse(visitor, context, programContext)
            }

            is InputNode -> {
                val hostData = programContext.get(it.host)
                val result = visitor.leave(it, hostData)

                // Update context
                val temporaryData = visitor.getData(it, hostData)
                context = context.put(it.temporary, temporaryData)

                result
            }

            is OutputNode -> {
                val message = it.message.traverse(visitor, context)
                visitor.leave(it, message, programContext.get(it.host))
            }

            is ReceiveNode -> {
                val protocolData = programContext.get(it.protocol)
                val result = visitor.leave(it, protocolData)

                // Update context
                val temporaryData = visitor.getData(it, protocolData)
                context = context.put(it.temporary, temporaryData)

                result
            }

            is SendNode -> {
                val message = it.message.traverse(visitor, context)
                visitor.leave(it, message, programContext.get(it.protocol))
            }
        }
    }

    return visitor.leave(this, statements)
}

/**
 * Traverses the program's abstract syntax tree in depth-first order producing
 * a result using [visitor].
 */
fun <StatementResult, DeclarationResult, ProgramResult, HostData, ProtocolData> ProgramNode.traverse(
    visitor: ProgramVisitorWithContext<StatementResult, DeclarationResult, ProgramResult, HostData, ProtocolData>
): ProgramResult {
    /** Host and process portions of the context. */
    val programContext = run {
        var context: ProgramContext<HostData, ProtocolData> = ProgramContext()
        for (declaration in declarations) {
            context = when (declaration) {
                is HostDeclarationNode ->
                    context.put(declaration.name, visitor.getData(declaration))

                is ProcessDeclarationNode ->
                    context.put(declaration.protocol, visitor.getData(declaration))
            }
        }
        context
    }

    val declarations = declarations.map {
        when (it) {
            is HostDeclarationNode ->
                visitor.leave(it)

            is ProcessDeclarationNode -> {
                visitor.leave(it) { visitor ->
                    it.body.traverse(
                        visitor,
                        StatementContext(),
                        programContext
                    )
                }
            }
        }
    }

    return visitor.leave(this, declarations)
}
