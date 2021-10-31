package edu.cornell.cs.apl.viaduct.passes

import edu.cornell.cs.apl.viaduct.errors.IncorrectNumberOfArgumentsError
import edu.cornell.cs.apl.viaduct.errors.InvalidConstructorCallError
import edu.cornell.cs.apl.viaduct.errors.JumpOutsideLoopScopeError
import edu.cornell.cs.apl.viaduct.protocols.MainProtocol
import edu.cornell.cs.apl.viaduct.syntax.Arguments
import edu.cornell.cs.apl.viaduct.syntax.FunctionName
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.JumpLabel
import edu.cornell.cs.apl.viaduct.syntax.JumpLabelNode
import edu.cornell.cs.apl.viaduct.syntax.LabelNode
import edu.cornell.cs.apl.viaduct.syntax.Located
import edu.cornell.cs.apl.viaduct.syntax.NameMap
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariable
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariableNode
import edu.cornell.cs.apl.viaduct.syntax.ProtocolNode
import edu.cornell.cs.apl.viaduct.syntax.Temporary
import edu.cornell.cs.apl.viaduct.syntax.TemporaryNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.Node
import edu.cornell.cs.apl.viaduct.util.FreshNameGenerator
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AssertionNode as IAssertionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AtomicExpressionNode as IAtomicExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BlockNode as IBlockNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BreakNode as IBreakNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode as IDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclassificationNode as IDeclassificationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.EndorsementNode as IEndorsementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExpressionArgumentNode as IExpressionArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExpressionNode as IExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.FunctionArgumentNode as IFunctionArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.FunctionCallNode as IFunctionCallNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.FunctionDeclarationNode as IFunctionDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.HostDeclarationNode as IHostDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.IfNode as IIfNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InfiniteLoopNode as IInfiniteLoopNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InputNode as IInputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode as ILetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LiteralNode as ILiteralNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ObjectDeclarationArgumentNode as IObjectDeclarationArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ObjectReferenceArgumentNode as IObjectReferenceArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OperatorApplicationNode as IOperatorApplicationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutParameterArgumentNode as IOutParameterArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutParameterConstructorInitializerNode as IOutParameterConstructorInitializerNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutParameterExpressionInitializerNode as IOutParameterExpressionInitializerNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutParameterInitializationNode as IOutParameterInitializationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutputNode as IOutputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ParameterNode as IParameterNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProcessDeclarationNode as IProcessDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode as IProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.QueryNode as IQueryNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReadNode as IReadNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.StatementNode as IStatementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.TopLevelDeclarationNode as ITopLevelDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode as IUpdateNode
import edu.cornell.cs.apl.viaduct.syntax.surface.AssertionNode as SAssertionNode
import edu.cornell.cs.apl.viaduct.syntax.surface.BlockNode as SBlockNode
import edu.cornell.cs.apl.viaduct.syntax.surface.BreakNode as SBreakNode
import edu.cornell.cs.apl.viaduct.syntax.surface.ConstructorCallNode as SConstructorCallNode
import edu.cornell.cs.apl.viaduct.syntax.surface.DeclarationNode as SDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.surface.DeclassificationNode as SDeclassificationNode
import edu.cornell.cs.apl.viaduct.syntax.surface.EndorsementNode as SEndorsementNode
import edu.cornell.cs.apl.viaduct.syntax.surface.ExpressionArgumentNode as SExpressionArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.surface.ExpressionNode as SExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.surface.ForLoopNode as SForLoopNode
import edu.cornell.cs.apl.viaduct.syntax.surface.FunctionArgumentNode as SFunctionArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.surface.FunctionCallNode as SFunctionCallNode
import edu.cornell.cs.apl.viaduct.syntax.surface.FunctionDeclarationNode as SFunctionDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.surface.HostDeclarationNode as SHostDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.surface.IfNode as SIfNode
import edu.cornell.cs.apl.viaduct.syntax.surface.InfiniteLoopNode as SInfiniteLoopNode
import edu.cornell.cs.apl.viaduct.syntax.surface.InputNode as SInputNode
import edu.cornell.cs.apl.viaduct.syntax.surface.LetNode as SLetNode
import edu.cornell.cs.apl.viaduct.syntax.surface.LiteralNode as SLiteralNode
import edu.cornell.cs.apl.viaduct.syntax.surface.ObjectDeclarationArgumentNode as SObjectDeclarationArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.surface.ObjectReferenceArgumentNode as SObjectReferenceArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.surface.OperatorApplicationNode as SOperatorApplicationNode
import edu.cornell.cs.apl.viaduct.syntax.surface.OutParameterArgumentNode as SOutParameterArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.surface.OutParameterInitializationNode as SOutParameterInitializationNode
import edu.cornell.cs.apl.viaduct.syntax.surface.OutputNode as SOutputNode
import edu.cornell.cs.apl.viaduct.syntax.surface.ProgramNode as SProgramNode
import edu.cornell.cs.apl.viaduct.syntax.surface.QueryNode as SQueryNode
import edu.cornell.cs.apl.viaduct.syntax.surface.ReadNode as SReadNode
import edu.cornell.cs.apl.viaduct.syntax.surface.SkipNode as SSkipNode
import edu.cornell.cs.apl.viaduct.syntax.surface.StatementNode as SStatementNode
import edu.cornell.cs.apl.viaduct.syntax.surface.UpdateNode as SUpdateNode
import edu.cornell.cs.apl.viaduct.syntax.surface.WhileLoopNode as SWhileLoopNode

/**
 * Elaborates this surface program into a program in the intermediate representation.
 *
 * See [Node] for the list of transformations performed.
 */
fun SProgramNode.elaborated(): IProgramNode {
    val declarations = mutableListOf<ITopLevelDeclarationNode>()

    val nameGenerator = FreshNameGenerator()

    // Used to check for duplicate definitions.
    var hosts = NameMap<Host, Boolean>()
    var functions = NameMap<FunctionName, Boolean>()

    for (declaration in this.declarations) {
        when (declaration) {
            is SHostDeclarationNode -> {
                hosts = hosts.put(declaration.name, true)
                declarations.add(
                    IHostDeclarationNode(
                        declaration.name,
                        declaration.authority,
                        declaration.sourceLocation
                    )
                )
            }

            is SFunctionDeclarationNode -> {
                functions = functions.put(declaration.name, true)
                if (declaration.name.value == FunctionName("main")) {
                    if (declaration.parameters.isNotEmpty())
                        throw IncorrectNumberOfArgumentsError(declaration.name, 0, declaration.parameters)
                    declarations.add(
                        IProcessDeclarationNode(
                            ProtocolNode(MainProtocol, declaration.name.sourceLocation),
                            StatementElaborator(nameGenerator).elaborate(declaration.body),
                            declaration.sourceLocation
                        )
                    )
                } else {
                    declarations.add(FunctionElaborator(nameGenerator).elaborate(declaration))
                }
            }
        }
    }

    return IProgramNode(declarations, this.sourceLocation)
}

private fun renameLabel(
    objectRenames: NameMap<ObjectVariable, ObjectVariable>,
    labelNode: LabelNode
): LabelNode {
    val renamer = { param: String ->
        objectRenames[Located(ObjectVariable(param), labelNode.sourceLocation)].name
    }

    return Located(labelNode.value.rename(renamer), labelNode.sourceLocation)
}

private class FunctionElaborator(
    val nameGenerator: FreshNameGenerator
) {
    fun elaborate(functionDecl: SFunctionDeclarationNode): IFunctionDeclarationNode {
        var objectRenames = NameMap<ObjectVariable, ObjectVariable>()

        val elaboratedParameters = mutableListOf<IParameterNode>()
        for (parameter in functionDecl.parameters) {
            val newName = ObjectVariable(nameGenerator.getFreshName(parameter.name.value.name))
            objectRenames = objectRenames.put(parameter.name, newName)
        }

        for (parameter in functionDecl.parameters) {
            val newLocatedName = Located(objectRenames[parameter.name], parameter.name.sourceLocation)
            val elaboratedParameter =
                IParameterNode(
                    newLocatedName,
                    parameter.parameterDirection,
                    parameter.className,
                    parameter.typeArguments,
                    parameter.labelArguments?.let {
                        Arguments(
                            it.map { arg -> renameLabel(objectRenames, arg) },
                            it.sourceLocation
                        )
                    },
                    parameter.protocol,
                    parameter.sourceLocation
                )
            elaboratedParameters.add(elaboratedParameter)
        }

        return IFunctionDeclarationNode(
            functionDecl.name,
            functionDecl.pcLabel?.let { renameLabel(objectRenames, it) },
            Arguments(elaboratedParameters, functionDecl.parameters.sourceLocation),
            StatementElaborator(nameGenerator, objectRenames).elaborate(functionDecl.body),
            functionDecl.sourceLocation
        )
    }
}

private class StatementElaborator(
    private val nameGenerator: FreshNameGenerator,

    // Maps old [Name]s to their new [Name]s.
    private var temporaryRenames: NameMap<Temporary, Temporary>,
    private var objectRenames: NameMap<ObjectVariable, ObjectVariable>,
    private val jumpLabelRenames: NameMap<JumpLabel, JumpLabel>,

    /** The label of the innermost loop surrounding the current context. */
    private val surroundingLoop: JumpLabel?
) {
    private companion object {
        const val TMP_NAME = "${'$'}tmp"
        const val LOOP_NAME = "loop"
    }

    constructor(nameGenerator: FreshNameGenerator) :
        this(nameGenerator, NameMap(), NameMap(), NameMap(), null)

    /** Constructor used by FunctionElaborator. */
    constructor(
        nameGenerator: FreshNameGenerator,
        objectRenames: NameMap<ObjectVariable, ObjectVariable>
    ) : this(nameGenerator, NameMap(), objectRenames, NameMap(), null)

    private fun copy(
        jumpLabelRenames: NameMap<JumpLabel, JumpLabel> = this.jumpLabelRenames,
        surroundingLoop: JumpLabel? = this.surroundingLoop
    ): StatementElaborator =
        StatementElaborator(
            nameGenerator,
            NameMap(), // Temporaries are local and reset at each block.
            objectRenames,
            jumpLabelRenames,
            surroundingLoop
        )

    /** Generates a new temporary whose name is based on [baseName]. */
    private fun freshTemporary(baseName: String? = null): Temporary =
        Temporary(nameGenerator.getFreshName(baseName ?: TMP_NAME))

    /**
     * Runs [producer] to get a statement, and prepends bindings generated by [producer]
     * before that statement.
     */
    private fun withBindings(producer: (MutableList<in IStatementNode>) -> IStatementNode): List<IStatementNode> {
        val bindings = mutableListOf<IStatementNode>()
        bindings.add(producer(bindings))
        return bindings
    }

    /**
     * Converts this surface expression into an intermediate expression in A-normal form.
     * Intermediate result are bound using let statements, which are appended to [bindings].
     */
    private fun SExpressionNode.toAnf(bindings: MutableList<in IStatementNode>): IExpressionNode {
        return when (this) {
            is SLiteralNode ->
                ILiteralNode(value, sourceLocation)

            is SReadNode ->
                IReadNode(TemporaryNode(temporaryRenames[temporary], sourceLocation))

            is SOperatorApplicationNode ->
                IOperatorApplicationNode(
                    operator,
                    Arguments(
                        arguments.map { it.toAnf(bindings).toAtomic(bindings) },
                        arguments.sourceLocation
                    ),
                    sourceLocation
                )

            is SQueryNode -> {
                IQueryNode(
                    ObjectVariableNode(objectRenames[variable], variable.sourceLocation),
                    query,
                    Arguments(
                        arguments.map { it.toAnf(bindings).toAtomic(bindings) },
                        arguments.sourceLocation
                    ),
                    sourceLocation
                )
            }

            is SDeclassificationNode ->
                IDeclassificationNode(
                    expression.toAnf(bindings).toAtomic(bindings),
                    fromLabel,
                    toLabel,
                    sourceLocation
                )

            is SEndorsementNode ->
                IEndorsementNode(
                    expression.toAnf(bindings).toAtomic(bindings),
                    fromLabel,
                    toLabel,
                    sourceLocation
                )

            is SInputNode ->
                IInputNode(type, host, sourceLocation)

            is SConstructorCallNode ->
                throw InvalidConstructorCallError(this)
        }
    }

    private fun SFunctionArgumentNode.run(bindings: MutableList<in IStatementNode>): IFunctionArgumentNode =
        when (this) {
            is SExpressionArgumentNode ->
                IExpressionArgumentNode(
                    expression.toAnf(bindings).toAtomic(bindings),
                    sourceLocation
                )

            is SObjectDeclarationArgumentNode -> {
                val newName = ObjectVariable(nameGenerator.getFreshName(variable.value.name))
                objectRenames = objectRenames.put(variable, newName)
                IObjectDeclarationArgumentNode(
                    Located(newName, variable.sourceLocation),
                    sourceLocation
                )
            }

            is SObjectReferenceArgumentNode ->
                IObjectReferenceArgumentNode(
                    Located(objectRenames[variable], variable.sourceLocation),
                    sourceLocation
                )

            is SOutParameterArgumentNode ->
                IOutParameterArgumentNode(
                    Located(objectRenames[parameter], parameter.sourceLocation),
                    sourceLocation
                )
        }

    /**
     * Convert this expression to an atomic expression by introducing a new let binding
     * if necessary. This binding is appended to [bindings].
     */
    private fun IExpressionNode.toAtomic(bindings: MutableList<in ILetNode>): IAtomicExpressionNode {
        return when (this) {
            is IAtomicExpressionNode ->
                this

            else -> {
                val tmp = TemporaryNode(freshTemporary(), this.sourceLocation)
                bindings.add(ILetNode(tmp, this, null, this.sourceLocation))
                IReadNode(tmp)
            }
        }
    }

    /** Converts surface block statement into an intermediate block statement. */
    fun elaborate(block: SBlockNode): IBlockNode {
        val newScope = this.copy()

        // Flatten children into one big list
        val statements = mutableListOf<IStatementNode>()
        block.forEach { statements.addAll(newScope.run(it)) }

        return IBlockNode(statements, block.sourceLocation)
    }

    private fun run(stmt: SStatementNode): List<IStatementNode> {
        return when (stmt) {
            is SLetNode ->
                withBindings { bindings ->
                    // The value must be processed before the temporary is freshened
                    val newValue = stmt.value.toAnf(bindings)

                    val newName = freshTemporary(stmt.temporary.value.name)
                    temporaryRenames = temporaryRenames.put(stmt.temporary, newName)

                    ILetNode(
                        TemporaryNode(newName, stmt.temporary.sourceLocation),
                        newValue,
                        stmt.protocol,
                        stmt.sourceLocation
                    )
                }

            is SDeclarationNode ->
                when (val initializer = stmt.initializer) {
                    is SConstructorCallNode ->
                        withBindings { bindings ->
                            // The arguments must be processed before the variable is freshened
                            val newArguments =
                                Arguments(
                                    initializer.arguments.map { it.toAnf(bindings).toAtomic(bindings) },
                                    initializer.arguments.sourceLocation
                                )

                            val newName =
                                ObjectVariable(nameGenerator.getFreshName(stmt.variable.value.name))
                            objectRenames = objectRenames.put(stmt.variable, newName)

                            IDeclarationNode(
                                ObjectVariableNode(newName, stmt.variable.sourceLocation),
                                initializer.className,
                                initializer.typeArguments,
                                initializer.labelArguments?.let {
                                    Arguments(
                                        it.map { arg -> renameLabel(objectRenames, arg) },
                                        it.sourceLocation
                                    )
                                },
                                newArguments,
                                initializer.protocol,
                                stmt.sourceLocation
                            )
                        }

                    else ->
                        throw InvalidConstructorCallError(initializer, constructorNeeded = true)
                }

            is SUpdateNode ->
                withBindings { bindings ->
                    IUpdateNode(
                        ObjectVariableNode(
                            objectRenames[stmt.variable],
                            stmt.variable.sourceLocation
                        ),
                        stmt.update,
                        Arguments(
                            stmt.arguments.map { it.toAnf(bindings).toAtomic(bindings) },
                            stmt.arguments.sourceLocation
                        ),
                        stmt.sourceLocation
                    )
                }

            is SOutParameterInitializationNode ->
                withBindings { bindings ->
                    val newName = Located(objectRenames[stmt.name], stmt.name.sourceLocation)
                    val initializer =
                        when (val rhs = stmt.rhs) {
                            is SConstructorCallNode ->
                                IOutParameterConstructorInitializerNode(
                                    rhs.className,
                                    rhs.typeArguments,
                                    rhs.labelArguments?.let {
                                        Arguments(
                                            it.map { arg -> renameLabel(objectRenames, arg) },
                                            it.sourceLocation
                                        )
                                    },
                                    Arguments(
                                        rhs.arguments.map { it.toAnf(bindings).toAtomic(bindings) },
                                        rhs.arguments.sourceLocation
                                    ),
                                    rhs.sourceLocation
                                )

                            else ->
                                IOutParameterExpressionInitializerNode(
                                    rhs.toAnf(bindings).toAtomic(bindings),
                                    rhs.sourceLocation
                                )
                        }

                    IOutParameterInitializationNode(
                        newName,
                        initializer,
                        stmt.sourceLocation
                    )
                }

            is SFunctionCallNode ->
                withBindings { bindings ->
                    IFunctionCallNode(
                        stmt.name,
                        Arguments(
                            stmt.arguments.map { arg -> arg.run(bindings) },
                            stmt.arguments.sourceLocation
                        ),
                        stmt.sourceLocation
                    )
                }

            is SSkipNode ->
                listOf()

            is SOutputNode ->
                withBindings { bindings ->
                    IOutputNode(
                        stmt.message.toAnf(bindings).toAtomic(bindings),
                        stmt.host,
                        stmt.sourceLocation
                    )
                }

            is SAssertionNode ->
                withBindings { bindings ->
                    IAssertionNode(
                        stmt.condition.toAnf(bindings).toAtomic(bindings),
                        stmt.sourceLocation
                    )
                }

            is SIfNode -> {
                withBindings { bindings ->
                    IIfNode(
                        stmt.guard.toAnf(bindings).toAtomic(bindings),
                        elaborate(stmt.thenBranch),
                        elaborate(stmt.elseBranch),
                        stmt.sourceLocation
                    )
                }
            }

            is SInfiniteLoopNode -> {
                // Create a jump label if there isn't one
                val jumpLabel = stmt.jumpLabel?.value?.name ?: LOOP_NAME
                val renamedJumpLabel = JumpLabel(nameGenerator.getFreshName(jumpLabel))
                val jumpLabelLocation = stmt.jumpLabel?.sourceLocation ?: stmt.sourceLocation

                val newScope = this.copy(
                    jumpLabelRenames =
                    if (stmt.jumpLabel == null)
                        jumpLabelRenames
                    else jumpLabelRenames.put(stmt.jumpLabel, renamedJumpLabel),
                    surroundingLoop = renamedJumpLabel
                )

                listOf(
                    IInfiniteLoopNode(
                        newScope.elaborate(stmt.body),
                        JumpLabelNode(renamedJumpLabel, jumpLabelLocation),
                        stmt.sourceLocation
                    )
                )
            }

            is SBreakNode -> {
                if (surroundingLoop == null)
                    throw JumpOutsideLoopScopeError(stmt)

                val jumpLabelNode: JumpLabelNode =
                    if (stmt.jumpLabel == null)
                        JumpLabelNode(surroundingLoop, stmt.sourceLocation)
                    else
                        JumpLabelNode(
                            jumpLabelRenames[stmt.jumpLabel],
                            stmt.jumpLabel.sourceLocation
                        )

                listOf(
                    IBreakNode(jumpLabelNode, stmt.sourceLocation)
                )
            }

            is SWhileLoopNode ->
                run(elaborate(stmt))

            is SForLoopNode ->
                run(elaborate(stmt))

            is SBlockNode ->
                listOf(elaborate(stmt))
        }
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
