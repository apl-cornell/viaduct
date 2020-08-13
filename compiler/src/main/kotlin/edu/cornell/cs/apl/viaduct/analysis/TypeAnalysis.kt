package edu.cornell.cs.apl.viaduct.analysis

import edu.cornell.cs.apl.attributes.Tree
import edu.cornell.cs.apl.attributes.attribute
import edu.cornell.cs.apl.viaduct.errors.CompilationError
import edu.cornell.cs.apl.viaduct.errors.IncorrectNumberOfArgumentsError
import edu.cornell.cs.apl.viaduct.errors.ParameterDirectionMismatchError
import edu.cornell.cs.apl.viaduct.errors.TypeMismatchError
import edu.cornell.cs.apl.viaduct.errors.UnknownMethodError
import edu.cornell.cs.apl.viaduct.syntax.Arguments
import edu.cornell.cs.apl.viaduct.syntax.ClassNameNode
import edu.cornell.cs.apl.viaduct.syntax.Located
import edu.cornell.cs.apl.viaduct.syntax.Name
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariable
import edu.cornell.cs.apl.viaduct.syntax.ParameterDirection
import edu.cornell.cs.apl.viaduct.syntax.Temporary
import edu.cornell.cs.apl.viaduct.syntax.ValueTypeNode
import edu.cornell.cs.apl.viaduct.syntax.Variable
import edu.cornell.cs.apl.viaduct.syntax.datatypes.ImmutableCell
import edu.cornell.cs.apl.viaduct.syntax.datatypes.MutableCell
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Vector
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AssertionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BlockNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BreakNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DowngradeNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExpressionArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.FunctionCallNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.FunctionDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.IfNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InfiniteLoopNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LiteralNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.Node
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ObjectDeclaration
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ObjectDeclarationArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ObjectReferenceArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OperatorApplicationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutParameterArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutParameterConstructorInitializerNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutParameterExpressionInitializerNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutParameterInitializationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutParameterInitializerNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ParameterNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProcessDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.QueryNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReadNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReceiveNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SendNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.StatementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode
import edu.cornell.cs.apl.viaduct.syntax.types.BooleanType
import edu.cornell.cs.apl.viaduct.syntax.types.FunctionType
import edu.cornell.cs.apl.viaduct.syntax.types.ImmutableCellType
import edu.cornell.cs.apl.viaduct.syntax.types.MutableCellType
import edu.cornell.cs.apl.viaduct.syntax.types.ObjectType
import edu.cornell.cs.apl.viaduct.syntax.types.Type
import edu.cornell.cs.apl.viaduct.syntax.types.UnitType
import edu.cornell.cs.apl.viaduct.syntax.types.ValueType
import edu.cornell.cs.apl.viaduct.syntax.types.VectorType

/** Associates [Variable]s with their [Type]s. */
class TypeAnalysis private constructor(
    private val tree: Tree<Node, ProgramNode>,
    private val nameAnalysis: NameAnalysis
) {
    /** Throws [TypeMismatchError] if the type of this expression is not [expectedType]. */
    private fun ExpressionNode.assertHasType(expectedType: ValueType) {
        if (type != expectedType)
            throw TypeMismatchError(this, type, expectedType)
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
        if (methodType.arguments.size != arguments.size)
            throw IncorrectNumberOfArgumentsError(methodName, methodType.arguments.size, arguments)

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
                arguments.zip(operator.type.arguments) { argument, expectedType ->
                    argument.assertHasType(expectedType)
                }
                operator.type.result
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
            is ReceiveNode ->
                type.value
        }
    }

    private fun buildObjectType(className: ClassNameNode, typeArguments: Arguments<ValueTypeNode>): ObjectType {
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
    private val ObjectDeclaration.type: ObjectType by attribute {
        buildObjectType(className, typeArguments)
    }

    private val ParameterNode.type: ObjectType by attribute {
        buildObjectType(className, typeArguments)
    }

    /** See [type]. */
    private val OutParameterInitializerNode.type: ObjectType by attribute {
        when (this) {
            is OutParameterExpressionInitializerNode ->
                buildObjectType(
                    Located(ImmutableCell, this.sourceLocation),
                    Arguments.from(Located(this.expression.type, this.expression.sourceLocation))
                )

            is OutParameterConstructorInitializerNode ->
                buildObjectType(this.className, this.typeArguments)
        }
    }

    /** Returns the inferred type of [node]. */
    fun type(node: ExpressionNode): ValueType = node.type

    /** Returns the inferred type of the [Temporary] defined by [node]. */
    fun type(node: LetNode): ValueType = node.value.type

    /** Returns the type of the [ObjectVariable] defined by [node]. */
    fun type(node: ObjectDeclaration): ObjectType = node.type

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
                    checkMethodCall(node.className, constructorType, node.arguments)
                }
                is UpdateNode -> {
                    val methodType = nameAnalysis.declaration(node).type.getType(node.update.value)
                    if (methodType == null) {
                        val objectType = type(nameAnalysis.declaration(node))
                        throw UnknownMethodError(
                            node.variable,
                            node.update,
                            objectType,
                            node.arguments.map { it.type })
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

                        if ((if (isOutArgument) ParameterDirection.PARAM_OUT else ParameterDirection.PARAM_IN)
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
                is SendNode ->
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
        tree.root.filterIsInstance<ProcessDeclarationNode>().forEach { check(it.body) }
        tree.root.filterIsInstance<FunctionDeclarationNode>().forEach { check(it.body) }
    }

    companion object : AnalysisProvider<TypeAnalysis> {
        private val ProgramNode.instance: TypeAnalysis by attribute {
            TypeAnalysis(this.tree, NameAnalysis.get(this))
        }

        override fun get(program: ProgramNode): TypeAnalysis = program.instance
    }
}
