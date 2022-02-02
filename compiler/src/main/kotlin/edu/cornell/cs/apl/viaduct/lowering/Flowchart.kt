package edu.cornell.cs.apl.viaduct.lowering

import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariable
import edu.cornell.cs.apl.viaduct.syntax.Operator
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.Temporary
import edu.cornell.cs.apl.viaduct.syntax.datatypes.ClassName
import edu.cornell.cs.apl.viaduct.syntax.datatypes.QueryName
import edu.cornell.cs.apl.viaduct.syntax.datatypes.UpdateName
import edu.cornell.cs.apl.viaduct.syntax.types.ValueType
import edu.cornell.cs.apl.viaduct.syntax.values.Value
import kotlinx.collections.immutable.PersistentList

sealed class LoweredExpression

data class LiteralNode(val value: Value): LoweredExpression()

data class ReadNode(val temporary: Temporary): LoweredExpression()

data class OperatorApplicationNode(
    val operator: Operator,
    val arguments: PersistentList<LoweredExpression>
): LoweredExpression()

data class QueryNode(
    val variable: ObjectVariable,
    val query: QueryName,
    val arguments: PersistentList<LoweredExpression>
): LoweredExpression()

data class InputNode(
    val type: ValueType,
    val host: Host
): LoweredExpression()

sealed class LoweredStatement

object SkipNode: LoweredStatement()

data class DeclarationNode(
    val name: ObjectVariable,
    val className: ClassName,
    val typeArguments: PersistentList<ValueType>,
    val arguments: PersistentList<LoweredExpression>,
    val protocol: Protocol
): LoweredStatement()

data class LetNode(
    val temporary: Temporary,
    val value: LoweredExpression,
    val protocol: Protocol
): LoweredStatement()

data class UpdateNode(
    val variable: ObjectVariable,
    val update: UpdateName,
    val arguments: PersistentList<LoweredExpression>
): LoweredStatement()

data class OutputNode(
    val message: LoweredExpression,
    val host: Host
): LoweredStatement()

sealed class LoweredControl

data class Goto(val label: BlockLabel): LoweredControl()

data class GotoIf(
    val guard: LoweredExpression,
    val thenLabel: BlockLabel,
    val elseLabel: BlockLabel
): LoweredControl()

object Halt: LoweredControl()

typealias BlockLabel = String
data class ResidualBlockLabel(val label: BlockLabel, val store: PartialStore)

val ENTRY_POINT_LABEL: BlockLabel = "main"

data class LoweredBasicBlock(
    val statements: List<LoweredStatement>,
    val jump: LoweredControl
)

data class FlowchartProgram(
    val blocks: Map<BlockLabel, LoweredBasicBlock>
)
