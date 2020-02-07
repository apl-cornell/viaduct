package edu.cornell.cs.apl.viaduct.syntax.intermediate

import edu.cornell.cs.apl.viaduct.syntax.Context
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.JumpLabel
import edu.cornell.cs.apl.viaduct.syntax.Located
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariable
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.Temporary

/**
 * A suspended traversal of a [StatementNode]. Allows the node to be traversed 0 or more times.
 *
 * Note that there is no memoization, and the node will be traversed each time the function is
 * invoked. This is done deliberately to allow traversing the node multiple times.
 */
typealias SuspendedTraversal<ExpressionResult, StatementResult, TemporaryData, ObjectData, LoopData, HostData, ProtocolData> =
        (StatementVisitorWithContext<ExpressionResult, StatementResult, TemporaryData, ObjectData, LoopData, HostData, ProtocolData>) -> StatementResult

/**
 * Traverses the expression's abstract syntax tree in depth-first order producing
 * a result using [visitor].
 */
private fun <ExpressionResult, TemporaryData, ObjectData> ExpressionNode.traverse(
    visitor: ExpressionVisitorWithContext<ExpressionResult, TemporaryData, ObjectData>,
    context: Context<TemporaryData, ObjectData, *, *, *>
): ExpressionResult {
    return when (this) {
        is LiteralNode ->
            visitor.leave(this)

        is ReadNode ->
            visitor.leave(
                this,
                context.getTemporaryData(Located(this.temporary, this.sourceLocation))
            )

        is OperatorApplicationNode ->
            visitor.leave(this, this.arguments.map { arg -> arg.traverse(visitor, context) })

        is QueryNode ->
            visitor.leave(
                this,
                this.arguments.map { arg -> arg.traverse(visitor, context) },
                context.getObjectData(this.variable)
            )

        is DeclassificationNode ->
            visitor.leave(this, this.expression.traverse(visitor, context))

        is EndorsementNode ->
            visitor.leave(this, this.expression.traverse(visitor, context))
    }
}

/**
 * Traverses the statement's abstract syntax tree in depth-first order producing
 * a result using [visitor].
 */
private fun <ExpressionResult, StatementResult, TemporaryData, ObjectData, LoopData, HostData, ProtocolData> BlockNode.traverse(
    visitor: StatementVisitorWithContext<ExpressionResult, StatementResult, TemporaryData, ObjectData, LoopData, HostData, ProtocolData>,
    context: Context<TemporaryData, ObjectData, LoopData, HostData, ProtocolData>
): StatementResult {
    // Make [context] mutable inside the function
    @Suppress("NAME_SHADOWING")
    var context = context

    fun StatementNode.go(): StatementResult {
        return when (this) {
            is LetNode -> {
                val value = this.value.traverse(visitor, context)
                val result = visitor.leave(this, value)

                // Update context
                val data = visitor.getData(this, value)
                context = context.putTemporaryData(this.temporary, data)

                result
            }

            is DeclarationNode -> {
                val arguments = this.arguments.map { it.traverse(visitor, context) }
                val result = visitor.leave(this, arguments)

                // Update context
                val data = visitor.getData(this, arguments)
                context = context.putObjectData(this.variable, data)

                result
            }

            is UpdateNode -> {
                val arguments = this.arguments.map { it.traverse(visitor, context) }
                visitor.leave(this, arguments, context.getObjectData(this.variable))
            }

            is IfNode -> {
                val guard = this.guard.traverse(visitor, context)
                // Kotlin differentiates between a captured var and a val.
                // Copying into a val ensures changes to [context] will not affect the context
                // passes to the closures.
                val contextHere = context
                visitor.leave(
                    this,
                    guard,
                    { this.thenBranch.traverse(it, contextHere) },
                    { this.elseBranch.traverse(it, contextHere) })
            }

            is InfiniteLoopNode -> {
                val data = visitor.getData(this)
                val contextInBody = context.putLoopData(this.jumpLabel, data)
                visitor.leave(this, { this.body.traverse(it, contextInBody) }, data)
            }

            is BreakNode -> {
                visitor.leave(this, context.getLoopData(this.jumpLabel))
            }

            is BlockNode -> {
                this.traverse(visitor, context)
            }

            is InputNode -> {
                val hostData = context.getHostData(this.host)
                val result = visitor.leave(this, hostData)

                // Update context
                val temporaryData = visitor.getData(this, hostData)
                context = context.putTemporaryData(this.temporary, temporaryData)

                result
            }

            is OutputNode -> {
                val message = this.message.traverse(visitor, context)
                visitor.leave(this, message, context.getHostData(this.host))
            }

            is ReceiveNode -> {
                val protocolData = context.getProtocolData(this.protocol)
                val result = visitor.leave(this, protocolData)

                // Update context
                val temporaryData = visitor.getData(this, protocolData)
                context = context.putTemporaryData(this.temporary, temporaryData)

                result
            }

            is SendNode -> {
                val message = this.message.traverse(visitor, context)
                visitor.leave(this, message, context.getProtocolData(this.protocol))
            }
        }
    }

    val statements = this.statements.map(StatementNode::go)
    return visitor.leave(this, statements)
}

/**
 * Traverses the program's abstract syntax tree in depth-first order producing
 * a result using [visitor].
 */
fun <ExpressionResult, StatementResult, DeclarationResult, ProgramResult, TemporaryData, ObjectData, LoopData, HostData, ProtocolData> ProgramNode.traverse(
    visitor: ProgramVisitorWithContext<ExpressionResult, StatementResult, DeclarationResult, ProgramResult, TemporaryData, ObjectData, LoopData, HostData, ProtocolData>
): ProgramResult {
    /** Host and process portions of the context. */
    val context = run {
        var context: Context<TemporaryData, ObjectData, LoopData, HostData, ProtocolData> =
            Context()
        for (declaration in this.declarations) {
            context = when (declaration) {
                is HostDeclarationNode ->
                    context.putHostData(declaration.name, visitor.getData(declaration))

                is ProcessDeclarationNode ->
                    context.putProtocolData(declaration.protocol, visitor.getData(declaration))
            }
        }
        context
    }

    val declarations = this.declarations.map {
        when (it) {
            is HostDeclarationNode ->
                visitor.leave(it)

            is ProcessDeclarationNode -> {
                val body = it.body.traverse(visitor, context)
                visitor.leave(it, body)
            }
        }
    }

    return visitor.leave(this, declarations)
}

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
        thenBranch: SuspendedTraversal<ExpressionResult, StatementResult, TemporaryData, ObjectData, LoopData, HostData, ProtocolData>,
        elseBranch: SuspendedTraversal<ExpressionResult, StatementResult, TemporaryData, ObjectData, LoopData, HostData, ProtocolData>
    ): StatementResult

    fun leave(
        node: InfiniteLoopNode,
        body: SuspendedTraversal<ExpressionResult, StatementResult, TemporaryData, ObjectData, LoopData, HostData, ProtocolData>,
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
 * @param ExpressionResult Data returned from each [ExpressionNode].
 * @param StatementResult Data returned from each [StatementNode].
 * @param DeclarationResult Data returned from each [TopLevelDeclarationNode].
 * @param ProgramResult Data returned from the [ProgramNode].
 * @param TemporaryData Context information attached to each [Temporary] declaration.
 * @param ObjectData Context information attached to each [ObjectVariable] declaration.
 * @param LoopData Context information attached to each [JumpLabel].
 * @param HostData Context information attached to each [Host] declaration.
 * @param ProtocolData Context information attached to each [Protocol] declaration.
 */
interface ProgramVisitorWithContext<ExpressionResult, StatementResult, DeclarationResult, ProgramResult, TemporaryData, ObjectData, LoopData, HostData, ProtocolData>
    :
    StatementVisitorWithContext<ExpressionResult, StatementResult, TemporaryData, ObjectData, LoopData, HostData, ProtocolData> {
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

    fun leave(node: ProcessDeclarationNode, body: StatementResult): DeclarationResult

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
 * A statement visitor that uses context information for variables and loops.
 *
 * @see StatementVisitorWithContext
 */
interface StatementVisitorWithVariableLoopContext<ExpressionResult, StatementResult, TemporaryData, ObjectData, LoopData>
    : StatementVisitorWithContext<ExpressionResult, StatementResult, TemporaryData, ObjectData, LoopData, Unit, Unit> {

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
 * A statement visitor that uses context information for variables.
 *
 * @see StatementVisitorWithContext
 */
interface StatementVisitorWithVariableContext<ExpressionResult, StatementResult, TemporaryData, ObjectData>
    : StatementVisitorWithVariableLoopContext<ExpressionResult, StatementResult, TemporaryData, ObjectData, Unit> {

    override fun getData(node: InfiniteLoopNode) {}

    override fun leave(node: BreakNode, data: Unit): StatementResult {
        return leave(node)
    }

    fun leave(node: BreakNode): StatementResult
}

/**
 * A statement visitor that does not use context information.
 *
 * @see StatementVisitorWithContext
 */
interface StatementVisitor<ExpressionResult, StatementResult> :
    ExpressionVisitor<ExpressionResult>,
    StatementVisitorWithContext<ExpressionResult, StatementResult, Unit, Unit, Unit, Unit, Unit> {
    override fun getData(node: LetNode, value: ExpressionResult) {}

    override fun getData(node: DeclarationNode, arguments: List<ExpressionResult>) {}

    override fun getData(node: InfiniteLoopNode) {}

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
        thenBranch: SuspendedTraversal<ExpressionResult, StatementResult, Unit, Unit, Unit, Unit, Unit>,
        elseBranch: SuspendedTraversal<ExpressionResult, StatementResult, Unit, Unit, Unit, Unit, Unit>
    ): StatementResult {
        return leave(node, guard, thenBranch(this), elseBranch(this))
    }

    override fun leave(
        node: InfiniteLoopNode,
        body: SuspendedTraversal<ExpressionResult, StatementResult, Unit, Unit, Unit, Unit, Unit>,
        data: Unit
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
interface ProgramVisitor<ExpressionResult, StatementResult, DeclarationResult, ProgramResult> :
    StatementVisitor<ExpressionResult, StatementResult>,
    ProgramVisitorWithContext<ExpressionResult, StatementResult, DeclarationResult, ProgramResult, Unit, Unit, Unit, Unit, Unit> {
    override fun getData(node: HostDeclarationNode) {}

    override fun getData(node: ProcessDeclarationNode) {}
}
