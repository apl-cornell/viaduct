package edu.cornell.cs.apl.viaduct.lowering

import com.ibm.icu.text.CaseMap
import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.PrettyPrintable
import edu.cornell.cs.apl.prettyprinting.bracketed
import edu.cornell.cs.apl.prettyprinting.concatenated
import edu.cornell.cs.apl.prettyprinting.plus
import edu.cornell.cs.apl.prettyprinting.times
import edu.cornell.cs.apl.prettyprinting.tupled
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
import java.util.LinkedList
import java.util.Queue

sealed class LoweredExpression : PrettyPrintable

data class LiteralNode(val value: Value) : LoweredExpression() {
    override fun toDocument(): Document = value.toDocument()
}

data class ReadNode(val temporary: Temporary) : LoweredExpression() {
    override fun toDocument(): Document = temporary.toDocument()
}

data class OperatorApplicationNode(
    val operator: Operator,
    val arguments: PersistentList<LoweredExpression>
) : LoweredExpression() {
    override fun toDocument(): Document = operator.toDocument(arguments)
}

data class QueryNode(
    val variable: ObjectVariable,
    val query: QueryName,
    val arguments: PersistentList<LoweredExpression>
) : LoweredExpression() {
    override fun toDocument(): Document =
        variable + Document(".") + query + arguments.tupled()
}

data class InputNode(
    val type: ValueType,
    val host: Host
) : LoweredExpression() {
    override fun toDocument(): Document =
        Document("input") * type * Document("from") * host
}

sealed class LoweredStatement : PrettyPrintable

object SkipNode : LoweredStatement() {
    override fun toDocument(): Document = Document("skip")
}

data class DeclarationNode(
    val name: ObjectVariable,
    val className: ClassName,
    val typeArguments: PersistentList<ValueType>,
    val arguments: PersistentList<LoweredExpression>,
    val protocol: Protocol
) : LoweredStatement() {
    override fun toDocument(): Document =
        Document("val") * name + Document("@") + protocol *
            Document("=") * className + typeArguments.bracketed() + arguments.tupled()
}

data class LetNode(
    val temporary: Temporary,
    val value: LoweredExpression,
    val protocol: Protocol
) : LoweredStatement() {
    override fun toDocument(): Document =
        Document("let") * temporary + Document("@") + protocol * Document("=") * value
}

data class UpdateNode(
    val variable: ObjectVariable,
    val update: UpdateName,
    val arguments: PersistentList<LoweredExpression>
) : LoweredStatement() {
    override fun toDocument(): Document =
        variable + Document(".") + update + arguments.tupled()
}

data class OutputNode(
    val message: LoweredExpression,
    val host: Host
) : LoweredStatement() {
    override fun toDocument(): Document =
        Document("output") * message * Document("to") * host
}

sealed class LoweredControl<T : BlockLabel> : PrettyPrintable {
    abstract fun successors(): Set<T>
}

data class Goto<T : BlockLabel>(val label: T) : LoweredControl<T>() {
    override fun toDocument(): Document =
        Document("goto") * label

    override fun successors(): Set<T> = setOf(label)
}

data class GotoIf<T : BlockLabel>(
    val guard: LoweredExpression,
    val thenLabel: T,
    val elseLabel: T
) : LoweredControl<T>() {
    override fun toDocument(): Document =
        Document("if") * guard *
            Document("then goto") * thenLabel *
            Document("else goto") * elseLabel

    override fun successors(): Set<T> = setOf(thenLabel, elseLabel)
}

// HACK! need a singleton paramerized on type T : BlockLabel,
// but Kotlin doesn't allow generic singletons. So instead
// we create two different singletons for each known BlockLabel class

object RegularHalt : LoweredControl<RegularBlockLabel>() {
    override fun toDocument(): Document = Document("halt")
    override fun successors(): Set<RegularBlockLabel> = setOf()
}

object ResidualHalt : LoweredControl<ResidualBlockLabel>() {
    override fun toDocument(): Document = Document("halt")
    override fun successors(): Set<ResidualBlockLabel> = setOf()
}

sealed class BlockLabel : PrettyPrintable

data class RegularBlockLabel(val label: String) : BlockLabel() {
    override fun toDocument(): Document = Document(label)
}

data class ResidualBlockLabel(val label: RegularBlockLabel, val store: PartialStore) : BlockLabel() {
    override fun toDocument(): Document =
        listOf(label.toDocument(), store).tupled()
}

val ENTRY_POINT_LABEL = RegularBlockLabel("main")

data class LoweredBasicBlock<T : BlockLabel>(
    val statements: List<LoweredStatement>,
    val jump: LoweredControl<T>
) : PrettyPrintable {
    override fun toDocument(): Document =
        if (statements.isNotEmpty()) {
            statements.concatenated(separator = Document.forcedLineBreak) + Document.forcedLineBreak + jump
        } else {
           jump.toDocument()
        }

    fun successors(): Set<T> = jump.successors()
}

data class FlowchartProgram(
    val blocks: Map<RegularBlockLabel, LoweredBasicBlock<RegularBlockLabel>>
) : PrettyPrintable {
    /** Return the entry point basic block for the program. */
    val entryPointBlock: LoweredBasicBlock<RegularBlockLabel> =
        blocks[ENTRY_POINT_LABEL]!!

    /** Adjecency list representation of the program's CFG. */
    val successorMap: Map<RegularBlockLabel, Set<RegularBlockLabel>> by lazy {
        blocks.map { kv -> kv.key to kv.value.successors() }.toMap()
    }

    /** Like successorMap, but for predecessors. */
    val predecessorMap: Map<RegularBlockLabel, Set<RegularBlockLabel>> by lazy {
        val predecessors = mutableMapOf<RegularBlockLabel, MutableSet<RegularBlockLabel>>()

        for (label in successorMap.keys) {
            predecessors[label] = mutableSetOf()
        }

        for (kv in successorMap) {
            for (successor in kv.value) {
                predecessors[successor]!!.add(kv.key)
            }
        }

        predecessors
    }

    /** The set of basic blocks that transition to HALT and exit the program. */
    val exitPoints: Set<RegularBlockLabel> by lazy {
        blocks.filter { kv -> kv.value.jump is RegularHalt }.map { kv -> kv.key }.toSet()
    }

    fun block(label: RegularBlockLabel): LoweredBasicBlock<RegularBlockLabel>? =
        blocks[label]

    override fun toDocument(): Document {
        val blockOrder = mutableListOf<RegularBlockLabel>()
        val worklist: Queue<RegularBlockLabel> = LinkedList()
        worklist.add(ENTRY_POINT_LABEL)

        while (worklist.isNotEmpty()) {
            val label = worklist.remove()
            blockOrder.add(label)
            for (successor in blocks[label]!!.successors()) {
                if (!blockOrder.contains(successor)) {
                    worklist.add(successor)
                }
            }
        }

        return blockOrder.map { label ->
            Document("label") * label + ":" + Document.forcedLineBreak + blocks[label]!!
        }.concatenated(Document.forcedLineBreak + Document.forcedLineBreak)
    }
}
