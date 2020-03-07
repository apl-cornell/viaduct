package edu.cornell.cs.apl.viaduct.analysis

import edu.cornell.cs.apl.attributes.attribute
import edu.cornell.cs.apl.viaduct.errors.CompilationError
import edu.cornell.cs.apl.viaduct.errors.IncorrectNumberOfArgumentsError
import edu.cornell.cs.apl.viaduct.errors.TypeMismatchError
import edu.cornell.cs.apl.viaduct.syntax.Arguments
import edu.cornell.cs.apl.viaduct.syntax.Located
import edu.cornell.cs.apl.viaduct.syntax.Name
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariable
import edu.cornell.cs.apl.viaduct.syntax.Temporary
import edu.cornell.cs.apl.viaduct.syntax.Variable
import edu.cornell.cs.apl.viaduct.syntax.datatypes.ImmutableCell
import edu.cornell.cs.apl.viaduct.syntax.datatypes.MutableCell
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Vector
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AssertionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BlockNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BreakNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DowngradeNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.IfNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InfiniteLoopNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LiteralNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OperatorApplicationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProcessDeclarationNode
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
class TypeAnalysis(private val nameAnalysis: NameAnalysis) {
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
                // TODO: no such method error
                val methodType = nameAnalysis.declaration(this).type.getType(query.value)!!
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

    /** See [type]. */
    private val DeclarationNode.type: ObjectType by attribute {
        // TODO: move this somewhere else; unify.
        // TODO: error messages for missing type and label arguments
        when (className.value) {
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

    /** Returns the inferred type of [node]. */
    fun type(node: ExpressionNode): ValueType = node.type

    /** Returns the inferred type of the [Temporary] defined by [node]. */
    fun type(node: LetNode): ValueType = node.value.type

    /** Returns the type of the [ObjectVariable] defined by [node]. */
    fun type(node: DeclarationNode): ObjectType = node.type

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
                    // TODO: no such method error
                    val methodType = nameAnalysis.declaration(node).type.getType(node.update.value)
                    checkMethodCall(node.update, methodType!!, node.arguments)
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
        nameAnalysis.tree.root.filterIsInstance<ProcessDeclarationNode>().forEach { check(it.body) }
    }
}
