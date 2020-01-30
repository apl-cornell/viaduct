package edu.cornell.cs.apl.viaduct.syntax.intermediate

import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.JumpLabel
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariable
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.Temporary
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import java.util.Stack

/** Visitor for expressions that maintain context data for temporaries and objects. */
interface ExprContextVisitor<ExprT, TmpData, ObjData> {
    fun getTmpData(tmp: Temporary): TmpData

    fun getObjData(obj: ObjectVariable): ObjData

    fun leave(expr: LiteralNode): ExprT

    fun leave(expr: ReadNode, data: TmpData): ExprT

    fun leave(expr: OperatorApplicationNode, arguments: List<ExprT>): ExprT

    fun leave(expr: QueryNode, arguments: List<ExprT>, data: ObjData): ExprT

    fun leave(expr: DeclassificationNode, expression: ExprT): ExprT

    fun leave(expr: EndorsementNode, expression: ExprT): ExprT

    fun visit(expr: ExpressionNode): ExprT {
        return when (expr) {
            is LiteralNode -> leave(expr)

            is ReadNode -> leave(expr, getTmpData(expr.temporary))

            is OperatorApplicationNode -> leave(expr, expr.arguments.map { arg -> visit(arg) })

            is QueryNode -> {
                leave(
                    expr,
                    expr.arguments.map { arg -> visit(arg) },
                    getObjData(expr.variable.value))
            }

            is DeclassificationNode -> leave(expr, visit(expr.expression))

            is EndorsementNode -> leave(expr, visit(expr.expression))
        }
    }
}

/** Expression visitor without context data. */
interface ExprVisitor<ExprResult> : ExprContextVisitor<ExprResult, Unit, Unit> {
    override fun leave(expr: ReadNode, data: Unit): ExprResult {
        return leave(expr)
    }

    override fun leave(expr: QueryNode, arguments: List<ExprResult>, data: Unit): ExprResult {
        return leave(expr, arguments)
    }

    fun leave(expr: ReadNode): ExprResult

    fun leave(expr: QueryNode, arguments: List<ExprResult>): ExprResult
}

typealias StmtContextThunk<ExprT, StmtT, TmpData, ObjData, LoopData, HostData, ProcessData> =
        (StmtContextVisitor<ExprT, StmtT, TmpData, ObjData, LoopData, HostData, ProcessData>) -> StmtT

/** Statement visitor that maintains context data for a variety of names.
 *  This also allows custom traversal logic for control structures (loops and conditionals). */
interface StmtContextVisitor<ExprT, StmtT, TmpData, ObjData, LoopData, HostData, ProcessData>
    : ExprContextVisitor<ExprT, TmpData, ObjData>
{
    fun extract(stmt: LetNode, value: ExprT): TmpData

    fun extract(stmt: DeclarationNode, arguments: List<ExprT>): ObjData

    fun extract(stmt: InfiniteLoopNode): LoopData

    fun putTmpData(tmp: Temporary, data: TmpData)

    fun putObjData(obj: ObjectVariable, data: ObjData)

    fun putLoopData(loop: JumpLabel, data: LoopData)

    fun getLoopData(loop: JumpLabel): LoopData

    fun putHostData(host: Host, data: HostData)

    fun getHostData(host: Host): HostData

    fun putProcessData(process: Protocol, data: ProcessData)

    fun getProcessData(process: Protocol): ProcessData

    fun pushScope()

    fun popScope()

    fun leave(stmt: LetNode, value: ExprT, data: TmpData): StmtT

    fun leave(stmt: DeclarationNode, arguments: List<ExprT>, data: ObjData): StmtT

    fun leave(stmt: UpdateNode, arguments: List<ExprT>, data: ObjData): StmtT

    fun leave(
        stmt: IfNode,
        guard: ExprT,
        thenBranch: StmtContextThunk<ExprT, StmtT, TmpData, ObjData, LoopData, HostData, ProcessData>,
        elseBranch: StmtContextThunk<ExprT, StmtT, TmpData, ObjData, LoopData, HostData, ProcessData>
    ): StmtT

    fun leave(
        stmt: InfiniteLoopNode,
        body: StmtContextThunk<ExprT, StmtT, TmpData, ObjData, LoopData, HostData, ProcessData>,
        data: LoopData
    ): StmtT

    fun leave(stmt: BreakNode, data: LoopData): StmtT

    fun leave(stmt: BlockNode, statements: List<StmtT>): StmtT

    fun leave(stmt: InputNode, data: HostData): StmtT

    fun leave(stmt: OutputNode, message: ExprT, data: HostData): StmtT

    fun leave(stmt: SendNode, message: ExprT, data: ProcessData): StmtT

    fun leave(stmt: ReceiveNode, data: ProcessData): StmtT

    fun visit(stmt: StatementNode): StmtT {
        return when (stmt) {
            is LetNode -> {
                val valueResult = visit(stmt.value)
                val data = extract(stmt, valueResult)
                putTmpData(stmt.temporary.value, data)
                leave(stmt, valueResult, data)
            }

            is DeclarationNode -> {
                val argumentsResult = stmt.arguments.map { arg -> visit(arg) }
                val data = extract(stmt, argumentsResult)
                putObjData(stmt.variable.value, data)
                leave(stmt, argumentsResult, data)
            }

            is UpdateNode -> {
                leave(
                    stmt,
                    stmt.arguments.map { arg -> visit(arg) },
                    getObjData(stmt.variable.value)
                )
            }

            is IfNode -> {
                leave(
                    stmt,
                    visit(stmt.guard),
                    { v -> v.visit(stmt.thenBranch) },
                    { v -> v.visit(stmt.elseBranch) }
                )
            }

            is InfiniteLoopNode -> {
                val data = extract(stmt)
                putLoopData(stmt.jumpLabel, data)
                leave(stmt, { v -> v.visit(stmt.body) }, data)
            }

            is BreakNode -> {
                leave(stmt, getLoopData(stmt.jumpLabel))
            }

            is BlockNode -> {
                pushScope()
                val result = leave(stmt, stmt.statements.map { child -> visit(child) })
                popScope()
                result
            }

            is InputNode -> {
                leave(stmt, getHostData(stmt.host.value))
            }

            is OutputNode -> {
                leave(stmt, visit(stmt.message), getHostData(stmt.host.value))
            }

            is SendNode -> {
                leave(stmt, visit(stmt.message), getProcessData(stmt.protocol.value))
            }

            is ReceiveNode -> {
                leave(stmt, getProcessData(stmt.protocol.value))
            }
        }
    }
}

/** Statement visitor that fixes traversal logic for control structures. */
interface StrictStmtContextVisitor<ExprT, StmtT, TmpData, ObjData, LoopData, HostData, ProcessData>
    : StmtContextVisitor<ExprT, StmtT, TmpData, ObjData, LoopData, HostData, ProcessData>
{
    override fun leave(
        stmt: IfNode,
        guard: ExprT,
        thenBranch: StmtContextThunk<ExprT, StmtT, TmpData, ObjData, LoopData, HostData, ProcessData>,
        elseBranch: StmtContextThunk<ExprT, StmtT, TmpData, ObjData, LoopData, HostData, ProcessData>
    ) : StmtT {
        return leave(stmt, guard, thenBranch(this), elseBranch(this))
    }

    override fun leave(
        stmt: InfiniteLoopNode,
        body: StmtContextThunk<ExprT, StmtT, TmpData, ObjData, LoopData, HostData, ProcessData>,
        data: LoopData
    ) : StmtT {
        return leave(stmt, body(this), data)
    }

    fun leave(stmt: IfNode, guard: ExprT, thenBranch: StmtT, elseBranch: StmtT): StmtT

    fun leave(stmt: InfiniteLoopNode, body: StmtT, data: LoopData): StmtT
}

/** Statement visitor that maintains context data for variables and loops. */
interface VariableLoopContextVisitor<ExprT, StmtT, TmpData, ObjData, LoopData>
    : StmtContextVisitor<ExprT, StmtT, TmpData, ObjData, LoopData, Unit, Unit> {

    override fun getHostData(host: Host) {}

    override fun putHostData(host: Host, data: Unit) {}

    override fun getProcessData(process: Protocol) {}

    override fun putProcessData(process: Protocol, data: Unit) {}

    override fun leave(stmt: InputNode, data: Unit): StmtT {
        return leave(stmt)
    }

    override fun leave(stmt: OutputNode, message: ExprT, data: Unit): StmtT {
        return leave(stmt, message)
    }

    override fun leave(stmt: ReceiveNode, data: Unit): StmtT {
        return leave(stmt)
    }

    override fun leave(stmt: SendNode, message: ExprT, data: Unit): StmtT {
        return leave(stmt, message)
    }

    fun leave(stmt: InputNode): StmtT

    fun leave(stmt: OutputNode, message: ExprT): StmtT

    fun leave(stmt: ReceiveNode): StmtT

    fun leave(stmt: SendNode, message: ExprT): StmtT
}

/** Statement visitor that maintains context data for variables. */
interface VariableContextVisitor<ExprT, StmtT, TmpData, ObjData>
    : VariableLoopContextVisitor<ExprT, StmtT, TmpData, ObjData, Unit> {

    override fun extract(stmt: InfiniteLoopNode) {}

    override fun getLoopData(loop: JumpLabel) {}

    override fun putLoopData(loop: JumpLabel, data: Unit) {}

    override fun leave(
        stmt: InfiniteLoopNode,
        body: StmtContextThunk<ExprT, StmtT, TmpData, ObjData, Unit, Unit, Unit>,
        data: Unit
    ) : StmtT {
        return leave(stmt, body)
    }

    override fun leave(stmt: BreakNode, data: Unit): StmtT {
        return leave(stmt)
    }

    fun leave(
        stmt: InfiniteLoopNode,
        body: StmtContextThunk<ExprT, StmtT, TmpData, ObjData, Unit, Unit, Unit>
    ) : StmtT

    fun leave(stmt: BreakNode): StmtT
}

/** Statement visitor that doesn't maintain context data. */
interface StmtVisitor<ExprT, StmtT> : VariableContextVisitor<ExprT, StmtT, Unit, Unit> {
    override fun extract(stmt: LetNode, value: ExprT) {}

    override fun extract(stmt: DeclarationNode, arguments: List<ExprT>) {}

    override fun getTmpData(tmp: Temporary) {}

    override fun putTmpData(tmp: Temporary, data: Unit) {}

    override fun getObjData(obj: ObjectVariable) {}

    override fun putObjData(obj: ObjectVariable, data: Unit) {}

    override fun leave(stmt: DeclarationNode, arguments: List<ExprT>, data: Unit): StmtT {
        return leave(stmt, arguments)
    }

    override fun leave(stmt: UpdateNode, arguments: List<ExprT>, data: Unit): StmtT {
        return leave(stmt, arguments)
    }

    fun leave(stmt: DeclarationNode, arguments: List<ExprT>): StmtT

    fun leave(stmt: UpdateNode, arguments: List<ExprT>): StmtT
}

typealias StmtThunk<ExprT, StmtT> = StmtContextThunk<ExprT, StmtT, Unit, Unit, Unit, Unit, Unit>

/** Statement visitor that doesn't maintain context data
 * and fixes traversal logic for control structures. */
interface StrictStmtVisitor<ExprT, StmtT> :
    StmtVisitor<ExprT, StmtT>,
    StrictStmtContextVisitor<ExprT, StmtT, Unit, Unit, Unit, Unit, Unit>
{
    override fun leave(stmt: InfiniteLoopNode, body: StmtThunk<ExprT, StmtT>, data: Unit): StmtT {
        return leave(stmt, body(this))
    }

    override fun leave(stmt: InfiniteLoopNode, body: StmtThunk<ExprT, StmtT>) : StmtT {
        return leave(stmt, body(this))
    }

    override fun leave(stmt: InfiniteLoopNode, body: StmtT, data: Unit): StmtT {
        return leave(stmt, body)
    }

    fun leave(stmt: InfiniteLoopNode, body: StmtT): StmtT
}

/** Program visitor that extracts data from host and process top-level declarations. */
interface ProgramContextVisitor
<ExprT, StmtT, ProgramT, TmpData, ObjData, LoopData, HostData, ProcessData>
{
    val stmtVisitor: StmtContextVisitor<ExprT, StmtT, TmpData, ObjData, LoopData, HostData, ProcessData>

    fun extract(host: HostDeclarationNode): HostData

    fun extract(process: ProcessDeclarationNode): ProcessData

    fun leave(program: ProgramNode, processes: Map<Protocol, StmtT>): ProgramT

    fun visit(program: ProgramNode): ProgramT {
        for (kv in program.hosts) {
            val hostData = extract(kv.value)
            stmtVisitor.putHostData(kv.key, hostData)
        }

        for (kv in program.processes) {
            val processData = extract(kv.value)
            stmtVisitor.putProcessData(kv.key, processData)
        }

        val processResultMap = mutableMapOf<Protocol, StmtT>()
        for (kv in program.processes) {
            processResultMap[kv.key] = stmtVisitor.visit(kv.value.body)
        }

        return leave(program, processResultMap)
    }
}

/** Program visitor that does not maintain context information. */
interface ProgramVisitor<ExprT, StmtT, ProgramT>
    : ProgramContextVisitor<ExprT, StmtT, ProgramT, Unit, Unit, Unit, Unit, Unit>
{
    override val stmtVisitor: StmtVisitor<ExprT, StmtT>
}

/** Statement visitor that implements context handling. */
abstract class AbstractStmtContextVisitor
<ExprT, StmtT, ProgramT, TmpData, ObjData, LoopData, HostData, ProcessData>(
    private val contextStack: Stack<Context<TmpData, ObjData, LoopData, HostData, ProcessData>>
) : StmtContextVisitor<ExprT, StmtT, TmpData, ObjData, LoopData, HostData, ProcessData> {

    data class Context<TmpData, ObjData, LoopData, HostData, ProcessData>(
        val tmp: PersistentMap<Temporary, TmpData>,
        val obj: PersistentMap<ObjectVariable, ObjData>,
        val loop: PersistentMap<JumpLabel, LoopData>,
        val host: PersistentMap<Host, HostData>,
        val process: PersistentMap<Protocol, ProcessData>
    ) {
        constructor() :
            this(
                persistentMapOf(),
                persistentMapOf(),
                persistentMapOf(),
                persistentMapOf(),
                persistentMapOf()
            )
    }

    constructor() : this(Stack()) {
        contextStack.push(Context())
    }

    override fun pushScope() {
        contextStack.push(contextStack.peek())
    }

    override fun popScope() {
        contextStack.pop()
    }

    private var context: Context<TmpData, ObjData, LoopData, HostData, ProcessData>
        get() = contextStack.peek()
        set(value) {
            contextStack.pop()
            contextStack.push(value)
        }

    override fun getTmpData(tmp: Temporary): TmpData {
        return context.tmp[tmp]!!
    }

    override fun putTmpData(tmp: Temporary, data: TmpData) {
        context = context.copy(tmp = context.tmp.put(tmp, data))
    }

    override fun getObjData(obj: ObjectVariable): ObjData {
        return context.obj[obj]!!
    }

    override fun putObjData(obj: ObjectVariable, data: ObjData) {
        context = context.copy(obj = context.obj.put(obj, data))
    }

    override fun getLoopData(loop: JumpLabel): LoopData {
        return context.loop[loop]!!
    }

    override fun putLoopData(loop: JumpLabel, data: LoopData) {
        context = context.copy(loop = context.loop.put(loop, data))
    }

    override fun getHostData(host: Host): HostData {
        return context.host[host]!!
    }

    override fun putHostData(host: Host, data: HostData) {
        context = context.copy(host = context.host.put(host, data))
    }

    override fun getProcessData(process: Protocol): ProcessData {
        return context.process[process]!!
    }

    override fun putProcessData(process: Protocol, data: ProcessData) {
        context = context.copy(process = context.process.put(process, data))
    }
}

