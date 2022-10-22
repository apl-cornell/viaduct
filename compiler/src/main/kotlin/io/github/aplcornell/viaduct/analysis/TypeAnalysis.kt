package io.github.aplcornell.viaduct.analysis

import io.github.aplcornell.viaduct.attributes.Tree
import io.github.aplcornell.viaduct.attributes.attribute
import io.github.aplcornell.viaduct.errors.CompilationError
import io.github.aplcornell.viaduct.errors.IncorrectNumberOfArgumentsError
import io.github.aplcornell.viaduct.errors.ParameterDirectionMismatchError
import io.github.aplcornell.viaduct.errors.TypeMismatchError
import io.github.aplcornell.viaduct.errors.UnknownMethodError
import io.github.aplcornell.viaduct.syntax.Arguments
import io.github.aplcornell.viaduct.syntax.Located
import io.github.aplcornell.viaduct.syntax.Name
import io.github.aplcornell.viaduct.syntax.ObjectTypeNode
import io.github.aplcornell.viaduct.syntax.ObjectVariable
import io.github.aplcornell.viaduct.syntax.ParameterDirection
import io.github.aplcornell.viaduct.syntax.Temporary
import io.github.aplcornell.viaduct.syntax.Variable
import io.github.aplcornell.viaduct.syntax.datatypes.ImmutableCell
import io.github.aplcornell.viaduct.syntax.datatypes.MutableCell
import io.github.aplcornell.viaduct.syntax.datatypes.Vector
import io.github.aplcornell.viaduct.syntax.intermediate.AssertionNode
import io.github.aplcornell.viaduct.syntax.intermediate.BlockNode
import io.github.aplcornell.viaduct.syntax.intermediate.BreakNode
import io.github.aplcornell.viaduct.syntax.intermediate.DeclarationNode
import io.github.aplcornell.viaduct.syntax.intermediate.DowngradeNode
import io.github.aplcornell.viaduct.syntax.intermediate.ExpressionArgumentNode
import io.github.aplcornell.viaduct.syntax.intermediate.ExpressionNode
import io.github.aplcornell.viaduct.syntax.intermediate.FunctionCallNode
import io.github.aplcornell.viaduct.syntax.intermediate.FunctionDeclarationNode
import io.github.aplcornell.viaduct.syntax.intermediate.IfNode
import io.github.aplcornell.viaduct.syntax.intermediate.InfiniteLoopNode
import io.github.aplcornell.viaduct.syntax.intermediate.InputNode
import io.github.aplcornell.viaduct.syntax.intermediate.LetNode
import io.github.aplcornell.viaduct.syntax.intermediate.LiteralNode
import io.github.aplcornell.viaduct.syntax.intermediate.Node
import io.github.aplcornell.viaduct.syntax.intermediate.ObjectDeclarationArgumentNode
import io.github.aplcornell.viaduct.syntax.intermediate.ObjectReferenceArgumentNode
import io.github.aplcornell.viaduct.syntax.intermediate.ObjectVariableDeclarationNode
import io.github.aplcornell.viaduct.syntax.intermediate.OperatorApplicationNode
import io.github.aplcornell.viaduct.syntax.intermediate.OutParameterArgumentNode
import io.github.aplcornell.viaduct.syntax.intermediate.OutParameterConstructorInitializerNode
import io.github.aplcornell.viaduct.syntax.intermediate.OutParameterExpressionInitializerNode
import io.github.aplcornell.viaduct.syntax.intermediate.OutParameterInitializationNode
import io.github.aplcornell.viaduct.syntax.intermediate.OutParameterInitializerNode
import io.github.aplcornell.viaduct.syntax.intermediate.OutputNode
import io.github.aplcornell.viaduct.syntax.intermediate.ProgramNode
import io.github.aplcornell.viaduct.syntax.intermediate.QueryNode
import io.github.aplcornell.viaduct.syntax.intermediate.ReadNode
import io.github.aplcornell.viaduct.syntax.intermediate.StatementNode
import io.github.aplcornell.viaduct.syntax.intermediate.UpdateNode
import io.github.aplcornell.viaduct.syntax.types.BooleanType
import io.github.aplcornell.viaduct.syntax.types.FunctionType
import io.github.aplcornell.viaduct.syntax.types.ImmutableCellType
import io.github.aplcornell.viaduct.syntax.types.MutableCellType
import io.github.aplcornell.viaduct.syntax.types.ObjectType
import io.github.aplcornell.viaduct.syntax.types.Type
import io.github.aplcornell.viaduct.syntax.types.UnitType
import io.github.aplcornell.viaduct.syntax.types.ValueType
import io.github.aplcornell.viaduct.syntax.types.VectorType

/** Associates [Variable]s with their [Type]s. */
class TypeAnalysis private constructor(
    private val tree: Tree<Node, ProgramNode>,
    private val nameAnalysis: NameAnalysis
) {
    /** Throws [TypeMismatchError] if the type of this expression is not [expectedType]. */
    private fun ExpressionNode.assertHasType(expectedType: ValueType) {
        if (type != expectedType) {
            throw TypeMismatchError(this, type, expectedType)
        }
    }

    /**
     * Checks that a constructor or method call is well typed, and returns the result type.
     *
     * @throws TypeMismatchError if the call is not well typed.
     */
    private fun checkMethodCall(
        methodName: Located<Name>,
        methodType: FunctionType,
        arguments: Arguments<ExpressionNode>
    ): ValueType {
        if (methodType.arguments.size != arguments.size) {
            throw IncorrectNumberOfArgumentsError(methodName, methodType.arguments.size, arguments)
        }

        // Check that arguments have the correct types.
        arguments.zip(methodType.arguments) { argument, expectedType ->
            argument.assertHasType(expectedType)
        }

        return methodType.result
    }

    /** See [type]. */
    private val ExpressionNode.type: ValueType by attribute {
        when (this) {
            is LiteralNode ->
                value.type
            is ReadNode ->
                type(nameAnalysis.declaration(this))
            is OperatorApplicationNode -> {
                assert(arguments.size == operator.type.arguments.size)

                val possibleTypes = listOf(operator.type) + operator.alternativeTypes()

                var lastResultType: ValueType? = null
                var lastError: TypeMismatchError? = null

                for (functionType in possibleTypes) {
                    try {
                        arguments.zip(functionType.arguments) { argument, expectedType ->
                            argument.assertHasType(expectedType)
                        }
                        lastResultType = functionType.result
                        break
                    } catch (err: TypeMismatchError) {
                        lastError = err
                        continue
                    }
                }

                lastResultType ?: throw lastError!!
            }
            is QueryNode -> {
                val methodType = nameAnalysis.declaration(this).type.getType(query.value)
                if (methodType == null) {
                    val objectType = type(nameAnalysis.declaration(this))
                    throw UnknownMethodError(variable, query, objectType, arguments.map { it.type })
                }
                checkMethodCall(query, methodType, arguments)
            }
            is DowngradeNode ->
                expression.type
            is InputNode ->
                type.value
        }
    }

    private fun ObjectTypeNode.buildType(): ObjectType {
        // TODO: move this somewhere else; unify.
        // TODO: error messages for missing type and label arguments
        return when (className.value) {
            ImmutableCell -> {
                val elementType: ValueType = typeArguments[0].value
                ImmutableCellType(elementType)
            }
            MutableCell -> {
                val elementType: ValueType = typeArguments[0].value
                MutableCellType(elementType)
            }
            Vector -> {
                val elementType: ValueType = typeArguments[0].value
                VectorType(elementType)
            }
            else ->
                TODO("User defined classes.")
        }
    }

    /** See [type]. */
    private val ObjectVariableDeclarationNode.type: ObjectType by attribute {
        nameAnalysis.objectType(this).buildType()
    }

    /** See [type]. */
    private val OutParameterInitializerNode.type: ObjectType by attribute {
        when (this) {
            is OutParameterExpressionInitializerNode ->
                ObjectTypeNode(
                    Located(ImmutableCell, this.sourceLocation),
                    Arguments.from(Located(this.expression.type, this.expression.sourceLocation)),
                    null
                ).buildType()

            is OutParameterConstructorInitializerNode ->
                objectType.buildType()
        }
    }

    /** Returns the inferred type of [node]. */
    fun type(node: ExpressionNode): ValueType = node.type

    /** Returns the inferred type of the [Temporary] defined by [node]. */
    fun type(node: LetNode): ValueType = node.value.type

    /** Returns the type of the [ObjectVariable] defined by [node]. */
    fun type(node: ObjectVariableDeclarationNode): ObjectType = node.type

    fun type(node: OutParameterInitializationNode): ObjectType = node.initializer.type

    /** Asserts that the program is well typed, and throws [CompilationError] otherwise. */
    fun check() {
        fun check(node: StatementNode): Any {
            // Returning here forces the pattern match to be exhaustive.
            return when (node) {
                is LetNode ->
                    type(node)
                is DeclarationNode -> {
                    val constructorType = FunctionType(type(node).constructorArguments, UnitType)
                    checkMethodCall(node.objectType.className, constructorType, node.arguments)
                }
                is UpdateNode -> {
                    val methodType = nameAnalysis.declaration(node).type.getType(node.update.value)
                    if (methodType == null) {
                        val objectType = type(nameAnalysis.declaration(node))
                        throw UnknownMethodError(
                            node.variable,
                            node.update,
                            objectType,
                            node.arguments.map { it.type }
                        )
                    }
                    checkMethodCall(node.update, methodType, node.arguments)
                }

                is OutParameterInitializationNode -> {
                    val parameterDecl = nameAnalysis.declaration(node)
                    val parameterType = parameterDecl.type
                    val initializationType = node.initializer.type
                    val arguments =
                        when (val initializer = node.initializer) {
                            is OutParameterExpressionInitializerNode ->
                                Arguments.from(initializer.expression)

                            is OutParameterConstructorInitializerNode ->
                                initializer.arguments
                        }
                    val constructorType = FunctionType(initializationType.constructorArguments, UnitType)

                    if (parameterType != initializationType) {
                        throw TypeMismatchError(node, initializationType, parameterType)
                    } else {
                        checkMethodCall(
                            Located(initializationType.className, node.initializer.sourceLocation),
                            constructorType,
                            arguments
                        )
                    }
                }

                is FunctionCallNode -> {
                    val functionDecl = nameAnalysis.declaration(node)
                    val parameterTypes = functionDecl.parameters.map { it.type }

                    if (node.arguments.size != parameterTypes.size) {
                        throw IncorrectNumberOfArgumentsError(node.name, functionDecl.parameters.size, node.arguments)
                    }

                    for (i in 0 until node.arguments.size) {
                        val argument = node.arguments[i]

                        // check first if the argument type matches with the parameter type
                        // e.g. out x and val y arguments are OUT parameters,
                        // the other two kinds of arguments are IN parameters
                        val (argumentType: Type, isOutArgument: Boolean) =
                            when (argument) {
                                is ObjectDeclarationArgumentNode ->
                                    Pair(parameterTypes[i], true)

                                is OutParameterArgumentNode ->
                                    Pair(nameAnalysis.declaration(argument).type, true)

                                is ExpressionArgumentNode ->
                                    Pair(
                                        ImmutableCellType(argument.expression.type),
                                        false
                                    )

                                is ObjectReferenceArgumentNode ->
                                    Pair(nameAnalysis.declaration(argument).type, false)
                            }

                        val parameter = functionDecl.parameters[i]
                        val expectedType = parameterTypes[i]

                        if ((if (isOutArgument) ParameterDirection.OUT else ParameterDirection.IN)
                            != parameter.parameterDirection
                        ) {
                            throw ParameterDirectionMismatchError(parameter, argument)
                        }

                        if (argumentType != expectedType) {
                            throw TypeMismatchError(argument, argumentType, expectedType)
                        }
                    }
                }

                is OutputNode ->
                    node.message.type

                is IfNode -> {
                    node.guard.assertHasType(BooleanType)
                    check(node.thenBranch)
                    check(node.elseBranch)
                }
                is InfiniteLoopNode ->
                    check(node.body)
                is BreakNode ->
                    Unit
                is AssertionNode ->
                    node.condition.assertHasType(BooleanType)
                is BlockNode ->
                    node.statements.forEach { check(it) }
            }
        }
        tree.root.filterIsInstance<FunctionDeclarationNode>().forEach { check(it.body) }
    }

    companion object : AnalysisProvider<TypeAnalysis> {
        private fun construct(program: ProgramNode) = TypeAnalysis(program.tree, NameAnalysis.get(program))

        override fun get(program: ProgramNode): TypeAnalysis = program.cached(::construct)
    }
}
