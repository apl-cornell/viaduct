package edu.cornell.cs.apl.viaduct.syntax.intermediate

import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.JumpLabel
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariable
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.Temporary
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import java.util.Stack

interface ExprContextVisitor<ExprT, TmpData, ObjData> : (ExpressionNode) -> ExprT {
    fun leave(expr: LiteralNode): ExprT

    fun leave(expr: ReadNode, data: TmpData): ExprT

    fun leave(expr: OperatorApplicationNode, arguments: List<ExprT>): ExprT

    fun leave(expr: QueryNode, arguments: List<ExprT>, data: ObjData): ExprT

    fun leave(expr: DeclassificationNode, expression: ExprT): ExprT

    fun leave(expr: EndorsementNode, expression: ExprT): ExprT
}

typealias StmtContextThunk<ExprT, StmtT, TmpData, ObjData, LoopData, HostData, ProcessData> =
        (StmtContextVisitor<ExprT, StmtT, TmpData, ObjData, LoopData, HostData, ProcessData>) -> StmtT

typealias StmtThunk<ExprT, StmtT> = StmtContextThunk<ExprT, StmtT, Unit, Unit, Unit, Unit, Unit>

interface StmtContextVisitor<ExprT, StmtT, TmpData, ObjData, LoopData, HostData, ProcessData>
    : (StatementNode) -> StmtT
{
    val exprVisitor: ExprContextVisitor<ExprT, TmpData, ObjData>

    fun extract(stmt: LetNode, value: ExprT): TmpData

    fun extract(stmt: DeclarationNode, arguments: List<ExprT>): ObjData

    fun extract(stmt: InfiniteLoopNode): LoopData

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

    fun visit(stmt: StatementNode): StmtT
}

interface ProgramContextVisitor<ExprT, StmtT, ProgramT, TmpData, ObjData, LoopData, HostData, ProcessData>
    : (ProgramNode) -> ProgramT
{
    val stmtVisitor: StmtContextVisitor<ExprT, StmtT, TmpData, ObjData, LoopData, HostData, ProcessData>

    fun extract(host: HostDeclarationNode): HostData

    fun extract(process: ProcessDeclarationNode): ProcessData

    fun leave(host: HostDeclarationNode): ProgramT

    fun leave(process: ProcessDeclarationNode, body: StmtT): ProgramT

    fun leave(
        program: ProgramNode, hosts: Map<Host, ProgramT>, processes: Map<Protocol, ProgramT>
    ): ProgramT
}

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

interface VariableLoopContextVisitor<ExprT, StmtT, TmpData, ObjData, LoopData>
    : StmtContextVisitor<ExprT, StmtT, TmpData, ObjData, LoopData, Unit, Unit> {
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

interface VariableContextVisitor<ExprT, StmtT, TmpData, ObjData>
    : VariableLoopContextVisitor<ExprT, StmtT, TmpData, ObjData, Unit> {
    override fun extract(stmt: InfiniteLoopNode) {}

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

interface StmtVisitor<ExprT, StmtT> : VariableContextVisitor<ExprT, StmtT, Unit, Unit> {
    override val exprVisitor: ExprVisitor<ExprT>

    override fun extract(stmt: LetNode, value: ExprT) {}

    override fun extract(stmt: DeclarationNode, arguments: List<ExprT>) {}

    override fun leave(stmt: DeclarationNode, arguments: List<ExprT>, data: Unit): StmtT {
        return leave(stmt, arguments)
    }

    override fun leave(stmt: UpdateNode, arguments: List<ExprT>, data: Unit): StmtT {
        return leave(stmt, arguments)
    }

    fun leave(stmt: DeclarationNode, arguments: List<ExprT>): StmtT

    fun leave(stmt: UpdateNode, arguments: List<ExprT>): StmtT
}

interface ProgramVisitor<ExprT, StmtT, ProgramT>
    : ProgramContextVisitor<ExprT, StmtT, ProgramT, Unit, Unit, Unit, Unit, Unit>
{
    override val stmtVisitor: StmtVisitor<ExprT, StmtT>
}

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

abstract class AbstractContextVisitor
<ExprT, StmtT, TmpData, ObjData, LoopData, HostData, ProcessData>(
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

    private fun pushScope() {
        contextStack.push(contextStack.peek())
    }

    private fun popScope() {
        contextStack.pop()
    }

    private var context: Context<TmpData, ObjData, LoopData, HostData, ProcessData>
        get() = contextStack.peek()
        set(value) {
            contextStack.pop()
            contextStack.push(value)
        }

    private var tmpContext: PersistentMap<Temporary, TmpData>
        get() = context.tmp
        set(value) {
            context = context.copy(tmp = value)
        }

    private var objContext: PersistentMap<ObjectVariable, ObjData>
        get() = context.obj
        set(value) {
            context = context.copy(obj = value)
        }

    private var loopContext: PersistentMap<JumpLabel, LoopData>
        get() = context.loop
        set(value) {
            context = context.copy(loop = value)
        }

    private var hostContext: PersistentMap<Host, HostData>
        get() = context.host
        set(value) {
            context = context.copy(host = value)
        }

    private var processContext: PersistentMap<Protocol, ProcessData>
        get() = context.process
        set(value) {
            context = context.copy(process = value)
        }

    final override fun visit(stmt: StatementNode): StmtT {
        return when (stmt) {
            is LetNode -> {
                val valueResult = exprVisitor(stmt.value)
                val data = extract(stmt, valueResult)
                tmpContext = tmpContext.put(stmt.temporary.value, data)
                leave(stmt, valueResult, data)
            }

            is DeclarationNode -> {
                val argumentsResult = stmt.arguments.map { arg -> exprVisitor(arg) }
                val data = extract(stmt, argumentsResult)
                objContext = objContext.put(stmt.variable.value, data)
                leave(stmt, argumentsResult, data)
            }

            is UpdateNode -> {
                leave(
                    stmt,
                    stmt.arguments.map { arg -> exprVisitor(arg) },
                    objContext[stmt.variable.value]!!
                )
            }

            is IfNode -> {
                leave(
                    stmt,
                    exprVisitor(stmt.guard),
                    { v -> v(stmt.thenBranch) },
                    { v -> v(stmt.elseBranch) }
                )
            }

            is InfiniteLoopNode -> {
                val data = extract(stmt)
                loopContext = loopContext.put(stmt.jumpLabel, data)
                leave(stmt, { v -> v(stmt.body) }, data)
            }

            is BreakNode -> {
                leave(stmt, loopContext[stmt.jumpLabel]!!)
            }

            is BlockNode -> {
                pushScope()
                val result = leave(stmt, stmt.statements.map { child -> visit(child) })
                popScope()
                result
            }

            is InputNode -> {
                leave(stmt, hostContext[stmt.host.value]!!)
            }

            is OutputNode -> {
                leave(stmt, exprVisitor(stmt.message), hostContext[stmt.host.value]!!)
            }

            is SendNode -> {
                leave(stmt, exprVisitor(stmt.message), processContext[stmt.protocol.value]!!)
            }

            is ReceiveNode -> {
                leave(stmt, processContext[stmt.protocol.value]!!)
            }
        }
    }
}

