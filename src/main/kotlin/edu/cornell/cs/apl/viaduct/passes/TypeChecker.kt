package edu.cornell.cs.apl.viaduct.passes

import edu.cornell.cs.apl.viaduct.errorskotlin.TypeMismatchError
import edu.cornell.cs.apl.viaduct.security.Label
import edu.cornell.cs.apl.viaduct.syntax.Arguments
import edu.cornell.cs.apl.viaduct.syntax.HasSourceLocation
import edu.cornell.cs.apl.viaduct.syntax.Temporary
import edu.cornell.cs.apl.viaduct.syntax.datatypes.MutableCell
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Vector
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BlockNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BreakNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclassificationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.EndorsementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.HostDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.IfNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InfiniteLoopNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LiteralNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OperatorApplicationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProcessDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramVisitor
import edu.cornell.cs.apl.viaduct.syntax.intermediate.QueryNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReadNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReceiveNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SendNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.StatementVisitorWithVariableContext
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SuspendedTraversal
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.traverse
import edu.cornell.cs.apl.viaduct.syntax.types.BooleanType
import edu.cornell.cs.apl.viaduct.syntax.types.FunctionType
import edu.cornell.cs.apl.viaduct.syntax.types.MutableCellType
import edu.cornell.cs.apl.viaduct.syntax.types.ObjectType
import edu.cornell.cs.apl.viaduct.syntax.types.UnitType
import edu.cornell.cs.apl.viaduct.syntax.types.ValueType
import edu.cornell.cs.apl.viaduct.syntax.types.VectorType

fun ProgramNode.typeCheck(): Map<Temporary, ValueType> {
    return this.traverse(TypeChecker())
}

private class TypeChecker :
    ProgramVisitor<Unit, Unit, Map<Temporary, ValueType>>,
    StatementVisitorWithVariableContext<ValueType, Unit, ValueType, ObjectType> {

    private val typeMap: MutableMap<Temporary, ValueType> = mutableMapOf()

    /** Infers the object type from a constructor call. */
    // TODO: move this somewhere else?
    private fun getObjectType(declaration: DeclarationNode): ObjectType {
        when (declaration.className.value) {
            MutableCell -> {
                val elementType: ValueType = declaration.typeArguments[0].value
                val elementLabel: Label? = declaration.labelArguments?.get(0)?.value
                return MutableCellType(elementType, elementLabel)
            }

            Vector -> {
                // TODO: error messages for missing type and label arguments
                val elementType: ValueType = declaration.typeArguments[0].value
                val elementLabel: Label? = declaration.labelArguments?.get(0)?.value
                val sizeLabel: Label? = null
                return VectorType(elementType, elementLabel, sizeLabel)
            }

            else ->
                TODO("User defined classes.")
        }
    }

    /** Assert that a node has the given type.  */
    private fun assertHasType(
        node: HasSourceLocation,
        actualType: ValueType,
        expectedType: ValueType
    ) {
        if (actualType != expectedType) {
            throw TypeMismatchError(node, actualType, expectedType)
        }
    }

    /** Checks that an operator or method application is well typed, and returns the result type. */
    private fun checkApplication(
        function: FunctionType,
        arguments: Arguments<HasSourceLocation>,
        argumentTypes: List<ValueType>
    ): ValueType {
        require(arguments.size == argumentTypes.size)

        if (function.arguments.size != argumentTypes.size) {
            TODO("mismatch error")
        }

        // Check that arguments have the correct types.
        for (i in argumentTypes.indices) {
            assertHasType(
                arguments[i],
                actualType = argumentTypes[i],
                expectedType = function.arguments[i]
            )
        }

        return function.result
    }

    override fun getData(node: LetNode, value: ValueType): ValueType {
        return value
    }

    override fun getData(node: DeclarationNode, arguments: List<ValueType>): ObjectType {
        return getObjectType(node)
    }

    override fun getData(node: InputNode): ValueType {
        return node.type.value
    }

    override fun getData(node: ReceiveNode): ValueType {
        return node.type.value
    }

    override fun leave(node: LiteralNode): ValueType {
        return node.value.type
    }

    override fun leave(node: OperatorApplicationNode, arguments: List<ValueType>): ValueType {
        return checkApplication(node.operator.type, node.arguments, arguments)
    }

    override fun leave(node: ReadNode, data: ValueType): ValueType {
        return data
    }

    override fun leave(node: QueryNode, arguments: List<ValueType>, data: ObjectType): ValueType {
        // TODO: add no such method error
        return checkApplication(data.getType(node.query)!!, node.arguments, arguments)
    }

    override fun leave(node: DeclassificationNode, expression: ValueType): ValueType {
        return expression
    }

    override fun leave(node: EndorsementNode, expression: ValueType): ValueType {
        return expression
    }

    override fun leave(node: LetNode, value: ValueType) {}

    override fun leave(node: DeclarationNode, arguments: List<ValueType>) {
        val constructorType = FunctionType(getObjectType(node).constructorArguments, UnitType)
        checkApplication(constructorType, node.arguments, arguments)
    }

    override fun leave(node: UpdateNode, arguments: List<ValueType>, data: ObjectType) {
        // TODO: add no such method error
        checkApplication(data.getType(node.update)!!, node.arguments, arguments)
    }

    override fun leave(
        node: IfNode,
        guard: ValueType,
        thenBranch: SuspendedTraversal<Unit, ValueType, ObjectType, Unit, Unit, Unit>,
        elseBranch: SuspendedTraversal<Unit, ValueType, ObjectType, Unit, Unit, Unit>
    ) {
        assertHasType(node.guard, actualType = guard, expectedType = BooleanType)
        thenBranch(this)
        elseBranch(this)
    }

    override fun leave(
        node: InfiniteLoopNode,
        body: SuspendedTraversal<Unit, ValueType, ObjectType, Unit, Unit, Unit>
    ) {
        body(this)
    }

    override fun leave(node: BreakNode) {}

    override fun leave(node: BlockNode, statements: List<Unit>) {}

    override fun leave(node: InputNode) {}

    override fun leave(node: OutputNode, message: ValueType) {}

    override fun leave(node: ReceiveNode) {}

    override fun leave(node: SendNode, message: ValueType) {}

    override fun leave(node: HostDeclarationNode) {}

    override fun leave(
        node: ProcessDeclarationNode,
        body: SuspendedTraversal<Unit, *, *, *, Unit, Unit>
    ) {
        body(this)
    }

    override fun leave(node: ProgramNode, declarations: List<Unit>): Map<Temporary, ValueType> {
        return typeMap
    }
}
