package io.github.apl_cornell.viaduct.passes

import io.github.apl_cornell.viaduct.errors.InvalidConstructorCallError
import io.github.apl_cornell.viaduct.errors.JumpOutsideLoopScopeError
import io.github.apl_cornell.viaduct.security.LabelAnd
import io.github.apl_cornell.viaduct.security.LabelBottom
import io.github.apl_cornell.viaduct.security.LabelConfidentiality
import io.github.apl_cornell.viaduct.security.LabelIntegrity
import io.github.apl_cornell.viaduct.security.LabelParameter
import io.github.apl_cornell.viaduct.security.LabelTop
import io.github.apl_cornell.viaduct.syntax.Arguments
import io.github.apl_cornell.viaduct.syntax.FunctionName
import io.github.apl_cornell.viaduct.syntax.Host
import io.github.apl_cornell.viaduct.syntax.JumpLabel
import io.github.apl_cornell.viaduct.syntax.JumpLabelNode
import io.github.apl_cornell.viaduct.syntax.LabelNode
import io.github.apl_cornell.viaduct.syntax.LabelVariable
import io.github.apl_cornell.viaduct.syntax.Located
import io.github.apl_cornell.viaduct.syntax.NameMap
import io.github.apl_cornell.viaduct.syntax.ObjectTypeNode
import io.github.apl_cornell.viaduct.syntax.ObjectVariable
import io.github.apl_cornell.viaduct.syntax.ObjectVariableNode
import io.github.apl_cornell.viaduct.syntax.SourceLocation
import io.github.apl_cornell.viaduct.syntax.Temporary
import io.github.apl_cornell.viaduct.syntax.TemporaryNode
import io.github.apl_cornell.viaduct.syntax.intermediate.Node
import io.github.apl_cornell.viaduct.util.FreshNameGenerator
import io.github.apl_cornell.viaduct.syntax.intermediate.AssertionNode as IAssertionNode
import io.github.apl_cornell.viaduct.syntax.intermediate.AtomicExpressionNode as IAtomicExpressionNode
import io.github.apl_cornell.viaduct.syntax.intermediate.AuthorityDelegationDeclarationNode as IAuthorityDelegationDeclarationNode
import io.github.apl_cornell.viaduct.syntax.intermediate.BlockNode as IBlockNode
import io.github.apl_cornell.viaduct.syntax.intermediate.BreakNode as IBreakNode
import io.github.apl_cornell.viaduct.syntax.intermediate.DeclarationNode as IDeclarationNode
import io.github.apl_cornell.viaduct.syntax.intermediate.DeclassificationNode as IDeclassificationNode
import io.github.apl_cornell.viaduct.syntax.intermediate.EndorsementNode as IEndorsementNode
import io.github.apl_cornell.viaduct.syntax.intermediate.ExpressionArgumentNode as IExpressionArgumentNode
import io.github.apl_cornell.viaduct.syntax.intermediate.ExpressionNode as IExpressionNode
import io.github.apl_cornell.viaduct.syntax.intermediate.FunctionArgumentNode as IFunctionArgumentNode
import io.github.apl_cornell.viaduct.syntax.intermediate.FunctionCallNode as IFunctionCallNode
import io.github.apl_cornell.viaduct.syntax.intermediate.FunctionDeclarationNode as IFunctionDeclarationNode
import io.github.apl_cornell.viaduct.syntax.intermediate.HostDeclarationNode as IHostDeclarationNode
import io.github.apl_cornell.viaduct.syntax.intermediate.IFCDelegationDeclarationNode as IIFCDelegationDeclarationNode
import io.github.apl_cornell.viaduct.syntax.intermediate.IfNode as IIfNode
import io.github.apl_cornell.viaduct.syntax.intermediate.InfiniteLoopNode as IInfiniteLoopNode
import io.github.apl_cornell.viaduct.syntax.intermediate.InputNode as IInputNode
import io.github.apl_cornell.viaduct.syntax.intermediate.LetNode as ILetNode
import io.github.apl_cornell.viaduct.syntax.intermediate.LiteralNode as ILiteralNode
import io.github.apl_cornell.viaduct.syntax.intermediate.ObjectDeclarationArgumentNode as IObjectDeclarationArgumentNode
import io.github.apl_cornell.viaduct.syntax.intermediate.ObjectReferenceArgumentNode as IObjectReferenceArgumentNode
import io.github.apl_cornell.viaduct.syntax.intermediate.OperatorApplicationNode as IOperatorApplicationNode
import io.github.apl_cornell.viaduct.syntax.intermediate.OutParameterArgumentNode as IOutParameterArgumentNode
import io.github.apl_cornell.viaduct.syntax.intermediate.OutParameterConstructorInitializerNode as IOutParameterConstructorInitializerNode
import io.github.apl_cornell.viaduct.syntax.intermediate.OutParameterExpressionInitializerNode as IOutParameterExpressionInitializerNode
import io.github.apl_cornell.viaduct.syntax.intermediate.OutParameterInitializationNode as IOutParameterInitializationNode
import io.github.apl_cornell.viaduct.syntax.intermediate.OutputNode as IOutputNode
import io.github.apl_cornell.viaduct.syntax.intermediate.ParameterNode as IParameterNode
import io.github.apl_cornell.viaduct.syntax.intermediate.ProgramNode as IProgramNode
import io.github.apl_cornell.viaduct.syntax.intermediate.QueryNode as IQueryNode
import io.github.apl_cornell.viaduct.syntax.intermediate.ReadNode as IReadNode
import io.github.apl_cornell.viaduct.syntax.intermediate.StatementNode as IStatementNode
import io.github.apl_cornell.viaduct.syntax.intermediate.TopLevelDeclarationNode as ITopLevelDeclarationNode
import io.github.apl_cornell.viaduct.syntax.intermediate.UpdateNode as IUpdateNode
import io.github.apl_cornell.viaduct.syntax.surface.AssertionNode as SAssertionNode
import io.github.apl_cornell.viaduct.syntax.surface.AuthorityDelegationDeclarationNode as SAuthorityDelegationDeclarationNode
import io.github.apl_cornell.viaduct.syntax.surface.BlockNode as SBlockNode
import io.github.apl_cornell.viaduct.syntax.surface.BreakNode as SBreakNode
import io.github.apl_cornell.viaduct.syntax.surface.ConstructorCallNode as SConstructorCallNode
import io.github.apl_cornell.viaduct.syntax.surface.DeclarationNode as SDeclarationNode
import io.github.apl_cornell.viaduct.syntax.surface.DeclassificationNode as SDeclassificationNode
import io.github.apl_cornell.viaduct.syntax.surface.DelegationDeclarationNode as SDelegationDeclarationNode
import io.github.apl_cornell.viaduct.syntax.surface.EndorsementNode as SEndorsementNode
import io.github.apl_cornell.viaduct.syntax.surface.ExpressionArgumentNode as SExpressionArgumentNode
import io.github.apl_cornell.viaduct.syntax.surface.ExpressionNode as SExpressionNode
import io.github.apl_cornell.viaduct.syntax.surface.ForLoopNode as SForLoopNode
import io.github.apl_cornell.viaduct.syntax.surface.FunctionArgumentNode as SFunctionArgumentNode
import io.github.apl_cornell.viaduct.syntax.surface.FunctionCallNode as SFunctionCallNode
import io.github.apl_cornell.viaduct.syntax.surface.FunctionDeclarationNode as SFunctionDeclarationNode
import io.github.apl_cornell.viaduct.syntax.surface.HostDeclarationNode as SHostDeclarationNode
import io.github.apl_cornell.viaduct.syntax.surface.IFCDelegationDeclarationNode as SIFCDelegationDeclarationNode
import io.github.apl_cornell.viaduct.syntax.surface.IfNode as SIfNode
import io.github.apl_cornell.viaduct.syntax.surface.InfiniteLoopNode as SInfiniteLoopNode
import io.github.apl_cornell.viaduct.syntax.surface.InputNode as SInputNode
import io.github.apl_cornell.viaduct.syntax.surface.LetNode as SLetNode
import io.github.apl_cornell.viaduct.syntax.surface.LiteralNode as SLiteralNode
import io.github.apl_cornell.viaduct.syntax.surface.ObjectDeclarationArgumentNode as SObjectDeclarationArgumentNode
import io.github.apl_cornell.viaduct.syntax.surface.ObjectReferenceArgumentNode as SObjectReferenceArgumentNode
import io.github.apl_cornell.viaduct.syntax.surface.OperatorApplicationNode as SOperatorApplicationNode
import io.github.apl_cornell.viaduct.syntax.surface.OutParameterArgumentNode as SOutParameterArgumentNode
import io.github.apl_cornell.viaduct.syntax.surface.OutParameterInitializationNode as SOutParameterInitializationNode
import io.github.apl_cornell.viaduct.syntax.surface.OutputNode as SOutputNode
import io.github.apl_cornell.viaduct.syntax.surface.ProgramNode as SProgramNode
import io.github.apl_cornell.viaduct.syntax.surface.QueryNode as SQueryNode
import io.github.apl_cornell.viaduct.syntax.surface.ReadNode as SReadNode
import io.github.apl_cornell.viaduct.syntax.surface.SkipNode as SSkipNode
import io.github.apl_cornell.viaduct.syntax.surface.StatementNode as SStatementNode
import io.github.apl_cornell.viaduct.syntax.surface.UpdateNode as SUpdateNode
import io.github.apl_cornell.viaduct.syntax.surface.WhileLoopNode as SWhileLoopNode

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
                        declaration.sourceLocation
                    )
                )
            }

            is SFunctionDeclarationNode -> {
                functions = functions.put(declaration.name, true)
                declarations.add(FunctionElaborator(nameGenerator).elaborate(declaration))
            }

            is SDelegationDeclarationNode -> {
                when (declaration) {
                    is SAuthorityDelegationDeclarationNode -> {
                        declarations.add(
                            IAuthorityDelegationDeclarationNode(
                                declaration.from,
                                declaration.to,
                                declaration.delegationProjection,
                                declaration.sourceLocation
                            )
                        )
                    }

                    is SIFCDelegationDeclarationNode -> {
                        declarations.add(
                            IIFCDelegationDeclarationNode(
                                declaration.from,
                                declaration.to,
                                declaration.delegationProjection,
                                declaration.sourceLocation
                            )
                        )
                    }

                    else -> {}
                }
            }
        }
    }

    return IProgramNode(declarations, this.sourceLocation)
}

/*private fun LabelNode.renameObjects(renames: NameMap<ObjectVariable, ObjectVariable>): LabelNode {
    val renamer = { param: String ->
        renames[Located(ObjectVariable(param), this.sourceLocation)].name
    }

    return Located(this.value.rename(renamer), this.sourceLocation)
}

private fun LabelNode.renameObjects(objectRenames: NameMap<ObjectVariable, ObjectVariable>): LabelNode {
    return this
}

private fun Arguments<LabelNode>.renameObjects(objectRenames: NameMap<ObjectVariable, ObjectVariable>): Arguments<LabelNode> =
    Arguments(this.map { it.renameObjects(objectRenames) }, this.sourceLocation)
*/
private fun ObjectTypeNode.renameObjects(rename: ObjectVariable, sourceLoc: SourceLocation): ObjectTypeNode {
    assert(labelArguments == null)
    return ObjectTypeNode(
        className, typeArguments,
        Arguments(
            listOf(Located(LabelParameter(LabelVariable(rename.name)), sourceLoc)),
            sourceLocation
        )
    )
}

private class FunctionElaborator(val nameGenerator: FreshNameGenerator) {
    fun elaborate(functionDecl: SFunctionDeclarationNode): IFunctionDeclarationNode {
        val objectRenames = functionDecl.parameters.fold(
            NameMap<ObjectVariable, ObjectVariable>()
        ) { map, parameter ->
            val newName = ObjectVariable(nameGenerator.getFreshName(parameter.name.value.name))
            map.put(parameter.name, newName)
        }

        // when label parameters are not explicitly defined, create label parameters with the same name as variables.
        /*val verbose = functionDecl.labelParameters != null*/

        val elaboratedParameters = functionDecl.parameters.map { parameter ->
            IParameterNode(
                Located(objectRenames[parameter.name], parameter.name.sourceLocation),
                parameter.parameterDirection,
                /*if (verbose) parameter.objectType
                else parameter.objectType.renameObjects(objectRenames[parameter.name], parameter.sourceLocation)*/
                parameter.objectType,
                parameter.protocol,
                parameter.sourceLocation
            )
        }

        val delegations: Arguments<IIFCDelegationDeclarationNode> =
            Arguments(
                if (functionDecl.labelConstraints == null) {
                    listOf()
                } else {
                    functionDecl.labelConstraints.map {
                        IIFCDelegationDeclarationNode(
                            it.from,
                            it.to,
                            it.delegationProjection,
                            it.sourceLocation
                        )
                    }
                } /*+
                    if (!verbose) {
                        functionDecl.parameters
                            .filter {
                                it.objectType.labelArguments != null
                            }
                            .map {
                                val labelParameter =
                                    LabelNode(
                                        LabelParameter(LabelVariable(objectRenames[it.name].name)),
                                        it.sourceLocation
                                    )
                                when (it.parameterDirection) {
                                    ParameterDirection.IN ->
                                        IDelegationDeclarationNode(
                                            labelParameter,
                                            it.objectType.labelArguments!!.first(),
                                            DelegationKind.IFC,
                                            DelegationProjection.BOTH,
                                            it.sourceLocation
                                        )

                                    ParameterDirection.OUT ->
                                        IDelegationDeclarationNode(
                                            it.objectType.labelArguments!!.first(),
                                            labelParameter,
                                            DelegationKind.IFC,
                                            DelegationProjection.BOTH,
                                            it.sourceLocation
                                        )
                                }
                            }
                    } else {
                        listOf()
                    }*/, functionDecl.name.sourceLocation
            )
        // TODO: remove default pc label when we have pc label inference ready
        return IFunctionDeclarationNode(
            functionDecl.name,
            functionDecl.labelParameters ?: Arguments(
                /*objectRenames.values.map { LabelVariableNode(LabelVariable(it.name), functionDecl.sourceLocation) }*/
                listOf(),
                functionDecl.name.sourceLocation
            ),
            Arguments(elaboratedParameters, functionDecl.parameters.sourceLocation),
            delegations,
            functionDecl.pcLabel ?: LabelNode(
                LabelAnd(LabelIntegrity(LabelBottom), LabelConfidentiality(LabelTop)),
                functionDecl.sourceLocation
            ),
            StatementElaborator(nameGenerator, objectRenames = objectRenames).elaborate(functionDecl.body),
            functionDecl.sourceLocation
        )
    }
}

private class StatementElaborator(
    private val nameGenerator: FreshNameGenerator,

    // Maps old [Name]s to their new [Name]s.
    private var temporaryRenames: NameMap<Temporary, Temporary> = NameMap(),
    private var objectRenames: NameMap<ObjectVariable, ObjectVariable> = NameMap(),
    private val jumpLabelRenames: NameMap<JumpLabel, JumpLabel> = NameMap(),

    /** The label of the innermost loop surrounding the current context. */
    private val surroundingLoop: JumpLabel? = null
) {
    private companion object {
        const val TMP_NAME = "${'$'}tmp"
        const val LOOP_NAME = "loop"
    }

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

                            val newName = ObjectVariable(nameGenerator.getFreshName(stmt.variable.value.name))
                            objectRenames = objectRenames.put(stmt.variable, newName)

                            IDeclarationNode(
                                ObjectVariableNode(newName, stmt.variable.sourceLocation),
                                initializer.objectType,
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
                                    rhs.objectType,
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
