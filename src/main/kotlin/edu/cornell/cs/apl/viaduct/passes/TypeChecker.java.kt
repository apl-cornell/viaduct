package edu.cornell.cs.apl.viaduct.passes

import edu.cornell.cs.apl.viaduct.errorskotlin.TypeCheckError
import edu.cornell.cs.apl.viaduct.syntax.Temporary
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
import edu.cornell.cs.apl.viaduct.syntax.intermediate.Node
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OperatorApplicationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProcessDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramVisitorWithContext
import edu.cornell.cs.apl.viaduct.syntax.intermediate.QueryNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReadNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReceiveNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SendNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.StatementVisitorWithVariableContext
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SuspendedTraversal
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.traverse
import edu.cornell.cs.apl.viaduct.syntax.types.BooleanType
import edu.cornell.cs.apl.viaduct.syntax.types.MethodSignature
import edu.cornell.cs.apl.viaduct.syntax.types.Type
import edu.cornell.cs.apl.viaduct.syntax.types.UnitType
import edu.cornell.cs.apl.viaduct.syntax.types.ValueType
import kotlinx.collections.immutable.toImmutableList

fun ProgramNode.typeCheck(): Map<Temporary, ValueType> {
    return this.traverse(TypeChecker())
}

private class TypeChecker :
    ProgramVisitorWithContext<ValueType, Unit, Unit, Map<Temporary, ValueType>, ValueType, Unit, Unit, Unit, Unit>,
    StatementVisitorWithVariableContext<ValueType, Unit, ValueType, Unit> {

    private val typeMap = mutableMapOf<Temporary, ValueType>()

    override fun leave(node: LiteralNode): ValueType {
        return node.value.type
    }

    private fun <R : Type> checkApplication(
        applicationNode: Node,
        arguments: List<Node>,
        argumentTypes: List<ValueType>,
        paramTypes: List<ValueType>,
        resultType: R
    ): R {
        if (arguments.size == argumentTypes.size && argumentTypes.size == paramTypes.size) {
            for (i in argumentTypes.indices) {
                if (argumentTypes[i] != paramTypes[i]) {
                    throw TypeCheckError(
                        arguments[i],
                        argumentTypes[i],
                        paramTypes[i]
                    )
                }
            }
        } else {
            throw TypeCheckError(
                applicationNode,
                MethodSignature(argumentTypes.toImmutableList(), resultType),
                MethodSignature(paramTypes.toImmutableList(), resultType)
            )
        }

        return resultType
    }

    override fun leave(node: OperatorApplicationNode, arguments: List<ValueType>): ValueType {
        return checkApplication(
            node, node.arguments, arguments, node.operator.type.arguments, node.operator.type.result
        )
    }

    override fun leave(node: ReadNode, data: ValueType): ValueType {
        return data
    }

    override fun leave(node: QueryNode, arguments: List<ValueType>, data: Unit): ValueType {
        return checkApplication(
            node,
            node.arguments,
            arguments,
            node.query.arguments,
            node.query.result
        )
    }

    override fun leave(node: DeclassificationNode, expression: ValueType): ValueType {
        return expression
    }

    override fun leave(node: EndorsementNode, expression: ValueType): ValueType {
        return expression
    }

    override fun leave(node: LetNode, value: ValueType) {}

    override fun leave(node: DeclarationNode, arguments: List<ValueType>) {
        checkApplication(node, node.arguments, arguments, node.constructor.arguments, UnitType)
    }

    override fun leave(node: BlockNode, statements: List<Unit>) {}

    override fun leave(node: UpdateNode, arguments: List<ValueType>, data: Unit) {
        checkApplication(node, node.arguments, arguments, node.update.arguments, UnitType)
    }

    override fun leave(
        node: IfNode,
        guard: ValueType,
        thenBranch: SuspendedTraversal<ValueType, Unit, ValueType, Unit, Unit, Unit, Unit>,
        elseBranch: SuspendedTraversal<ValueType, Unit, ValueType, Unit, Unit, Unit, Unit>
    ) {
        if (guard == BooleanType) {
            thenBranch(this)
            elseBranch(this)
        } else {
            throw TypeCheckError(node.guard, guard, BooleanType)
        }
    }

    override fun leave(
        node: InfiniteLoopNode,
        body: SuspendedTraversal<ValueType, Unit, ValueType, Unit, Unit, Unit, Unit>,
        data: Unit
    ) {
        body(this)
    }

    override fun leave(node: BreakNode) {}

    override fun leave(node: InputNode) {}

    override fun leave(node: OutputNode, message: ValueType) {}

    override fun leave(node: ReceiveNode) {}

    override fun leave(node: SendNode, message: ValueType) {}

    override fun getData(node: LetNode, value: ValueType): ValueType {
        return value
    }

    override fun getData(node: DeclarationNode, arguments: List<ValueType>) {}

    override fun getData(node: InputNode): ValueType {
        return node.type.value
    }

    override fun getData(node: ReceiveNode): ValueType {
        return node.type.value
    }

    override fun getData(node: HostDeclarationNode) {}

    override fun getData(node: ProcessDeclarationNode) {}

    override fun leave(node: HostDeclarationNode) {}

    override fun leave(node: ProcessDeclarationNode, body: Unit) {}

    override fun leave(node: ProgramNode, declarations: List<Unit>): Map<Temporary, ValueType> {
        return typeMap
    }
}
