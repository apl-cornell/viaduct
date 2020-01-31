package edu.cornell.cs.apl.viaduct.syntax.intermediate

import edu.cornell.cs.apl.viaduct.syntax.Variable
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import java.util.Stack

interface ExpressionVisitorOld<out E> {
    fun visit(expr: ExpressionNode): E
}

interface StatementVisitorOld<out S> {
    fun visit(stmt: StatementNode): S
}

/** Polyglot-style visitor that allows return type of children to vary with
 * the actual return type.
 */
interface GeneralAbstractExpressionVisitor<CVisitorT : ExpressionVisitorOld<CExprT>, ExprT, CExprT>
    : ExpressionVisitorOld<ExprT> {
    override fun visit(expr: ExpressionNode): ExprT {
        val visitor = enter(expr)
        return when (expr) {
            is LiteralNode -> leave(expr)

            is ReadNode -> leave(expr)

            is OperatorApplicationNode ->
                leave(expr, expr.arguments.map { arg -> visitor.visit(arg) })

            is QueryNode ->
                leave(expr, expr.arguments.map { arg -> visitor.visit(arg) })

            is DeclassificationNode -> leave(expr, visitor.visit(expr.expression))

            is EndorsementNode -> leave(expr, visitor.visit(expr.expression))
        }
    }

    /** The visitor that will visit expr's children. */
    fun enter(expr: ExpressionNode): CVisitorT

    fun leave(expr: LiteralNode): ExprT

    fun leave(expr: ReadNode): ExprT

    fun leave(expr: OperatorApplicationNode, arguments: List<CExprT>): ExprT

    fun leave(expr: QueryNode, arguments: List<CExprT>): ExprT

    fun leave(expr: DeclassificationNode, expression: CExprT): ExprT

    fun leave(expr: EndorsementNode, expression: CExprT): ExprT
}

/** Polyglot style-visitor where children's return type must be the same
 * as the actual return type. */
interface AbstractExpressionVisitor
<CVisitorT : AbstractExpressionVisitor<CVisitorT, ExprT>, ExprT>
    : GeneralAbstractExpressionVisitor<CVisitorT, ExprT, ExprT> {

    @Suppress("UNCHECKED_CAST")
    override fun enter(expr: ExpressionNode): CVisitorT {
        return this as CVisitorT
    }
}

/** Polyglot-style visitor that allows return type of children to vary with
 * the actual return type.
 * Can be parameterized across the visitor for expressions inside statements.
 */
interface GeneralAbstractStatementVisitor
<CVisitorT : StatementVisitorOld<CStmtT>, ExprT, StmtT, CStmtT>
    : StatementVisitorOld<StmtT> {

    /** The visitor that will visit expressions inside of statements. */
    val exprVisitor: ExpressionVisitorOld<ExprT>

    override fun visit(stmt: StatementNode): StmtT {
        val visitor = enter(stmt)
        return when (stmt) {
            is LetNode -> leave(stmt, exprVisitor.visit(stmt.value))

            is DeclarationNode -> leave(stmt, stmt.arguments.map { arg -> exprVisitor.visit(arg) })

            is UpdateNode -> leave(stmt, stmt.arguments.map { arg -> exprVisitor.visit(arg) })

            is IfNode ->
                leave(
                    stmt,
                    exprVisitor.visit(stmt.guard),
                    visitor.visit(stmt.thenBranch),
                    visitor.visit(stmt.elseBranch)
                )

            is InfiniteLoopNode -> leave(stmt, visitor.visit(stmt.body))

            is BreakNode -> leave(stmt)

            is BlockNode -> leave(stmt, stmt.statements.map { arg -> visitor.visit(arg) })

            is InputNode -> leave(stmt)

            is OutputNode -> leave(stmt, exprVisitor.visit(stmt.message))

            is SendNode -> leave(stmt, exprVisitor.visit(stmt.message))

            is ReceiveNode -> leave(stmt)
        }
    }

    /** The visitor that will visit stmt's children. */
    fun enter(stmt: StatementNode): CVisitorT

    fun leave(stmt: LetNode, value: ExprT): StmtT

    fun leave(stmt: DeclarationNode, arguments: List<ExprT>): StmtT

    fun leave(stmt: UpdateNode, arguments: List<ExprT>): StmtT

    fun leave(stmt: IfNode, guard: ExprT, thenBranch: CStmtT, elseBranch: CStmtT): StmtT

    fun leave(stmt: InfiniteLoopNode, body: CStmtT): StmtT

    fun leave(stmt: BreakNode): StmtT

    fun leave(stmt: BlockNode, statements: List<CStmtT>): StmtT

    fun leave(stmt: InputNode): StmtT

    fun leave(stmt: OutputNode, message: ExprT): StmtT

    fun leave(stmt: SendNode, message: ExprT): StmtT

    fun leave(stmt: ReceiveNode): StmtT
}

/** Polyglot style-visitor where children's return type must be the same
 * as the actual return type. */
interface AbstractStatementVisitor
<CVisitorT : AbstractStatementVisitor<CVisitorT, ExprT, StmtT>, ExprT, StmtT> :
    GeneralAbstractStatementVisitor<CVisitorT, ExprT, StmtT, StmtT> {
    @Suppress("UNCHECKED_CAST")
    override fun enter(stmt: StatementNode): CVisitorT {
        return this as CVisitorT
    }
}

/** A statement visitor that is also an expression visitor. */
interface CombinedAbstractStatementVisitor
<CVisitorT : CombinedAbstractStatementVisitor<CVisitorT, ExprT, StmtT>, ExprT, StmtT> :
    AbstractStatementVisitor<CVisitorT, ExprT, StmtT>,
    AbstractExpressionVisitor<CVisitorT, ExprT> {
    override val exprVisitor: ExpressionVisitorOld<ExprT>
        get() = this
}

/** A statement visitor that doesn't return anything for statements.
 *  Useful for doing imperative computations on the AST. */
interface VoidStatementVisitor<CVisitorT : VoidStatementVisitor<CVisitorT, ExprT>, ExprT>
    : AbstractStatementVisitor<CVisitorT, ExprT, Unit> {

    override fun leave(stmt: IfNode, guard: ExprT, thenBranch: Unit, elseBranch: Unit) {
        leave(stmt, guard)
    }

    override fun leave(stmt: InfiniteLoopNode, body: Unit) {
        return leave(stmt)
    }

    override fun leave(stmt: BlockNode, statements: List<Unit>) {
        return leave(stmt)
    }

    override fun leave(stmt: BreakNode) {}

    override fun leave(stmt: InputNode) {}

    override fun leave(stmt: ReceiveNode) {}

    fun leave(stmt: IfNode, guard: ExprT) {}

    fun leave(stmt: InfiniteLoopNode) {}

    fun leave(stmt: BlockNode) {}
}

/** A statement visitor that doesn't return anything for statements or expressions.
 *  Useful for doing imperative computations on the AST. */
interface VoidVisitor<CVisitorT : VoidVisitor<CVisitorT>> :
    VoidStatementVisitor<CVisitorT, Unit>, CombinedAbstractStatementVisitor<CVisitorT, Unit, Unit> {

    override fun leave(expr: OperatorApplicationNode, arguments: List<Unit>) {
        return leave(expr)
    }

    override fun leave(expr: QueryNode, arguments: List<Unit>) {
        return leave(expr)
    }

    override fun leave(expr: DeclassificationNode, expression: Unit) {
        return leave(expr)
    }

    override fun leave(expr: EndorsementNode, expression: Unit) {
        return leave(expr)
    }

    override fun leave(stmt: LetNode, value: Unit) {
        return leave(stmt)
    }

    override fun leave(stmt: DeclarationNode, arguments: List<Unit>) {
        return leave(stmt)
    }

    override fun leave(stmt: UpdateNode, arguments: List<Unit>) {
        return leave(stmt)
    }

    override fun leave(stmt: IfNode, guard: Unit) {
        return leave(stmt)
    }

    override fun leave(stmt: OutputNode, message: Unit) {
        return leave(stmt)
    }

    override fun leave(stmt: SendNode, message: Unit) {
        return leave(stmt)
    }

    fun leave(expr: OperatorApplicationNode) {}

    fun leave(expr: QueryNode) {}

    fun leave(expr: DeclassificationNode) {}

    fun leave(expr: EndorsementNode) {}

    fun leave(stmt: LetNode) {}

    fun leave(stmt: DeclarationNode) {}

    fun leave(stmt: UpdateNode) {}

    fun leave(stmt: IfNode) {}

    fun leave(stmt: OutputNode) {}

    fun leave(stmt: SendNode) {}
}

/** Visitor that maintains lexically-scoped context information. */
abstract class ContextVisitor
<CVisitorT : ContextVisitor<CVisitorT, ExprT, StmtT, ContextT>, ExprT, StmtT, ContextT>
    (protected var contextStack: Stack<ContextT>) :
    AbstractStatementVisitor<CVisitorT, ExprT, StmtT> {

    constructor(init: ContextT) : this(Stack()) {
        contextStack.push(init)
    }

    protected var context: ContextT
        get() = contextStack.peek()
        set(value) {
            contextStack.pop()
            contextStack.push(value)
        }

    protected open fun enterScope() {}

    protected open fun leaveScope() {}

    override fun visit(stmt: StatementNode): StmtT {
        return when (stmt) {
            is BlockNode -> {
                contextStack.push(contextStack.peek())
                enterScope()
                val result = super.visit(stmt)
                leaveScope()
                contextStack.pop()
                result
            }

            else -> super.visit(stmt)
        }
    }
}

typealias VariableContext<T> = Stack<PersistentMap<Variable, T>>

/** Visitor that maintains information about variables in scope. */
abstract class NameContextVisitor
<SelfT : NameContextVisitor<SelfT, ExprT, StmtT, ContextT>, ExprT, StmtT, ContextT>
    : ContextVisitor<SelfT, ExprT, StmtT, PersistentMap<Variable, ContextT>> {
    constructor(context: VariableContext<ContextT>) : super(context)

    constructor() : super(Stack()) {
        contextStack.push(persistentMapOf())
    }

    private fun put(v: Variable, contextVal: ContextT) {
        context = context.put(v, contextVal)
    }

    private fun get(v: Variable): ContextT {
        return context[v]
            ?: throw NullPointerException("no renaming found for object declaration ${v.name}")
    }

    override fun visit(stmt: StatementNode): StmtT {
        return when (stmt) {
            is LetNode -> {
                val tmpContext = extract(stmt)
                if (tmpContext != null) {
                    put(stmt.temporary.value, tmpContext)
                }
                super.visit(stmt)
            }

            is DeclarationNode -> {
                val objContext = extract(stmt)
                if (objContext != null) {
                    put(stmt.variable.value, objContext)
                }
                super.visit(stmt)
            }

            else -> super.visit(stmt)
        }
    }

    abstract fun extract(letNode: LetNode): ContextT?

    abstract fun extract(declarationNode: DeclarationNode): ContextT?
}

class NothingVisitor :
    ContextVisitor<NothingVisitor, Int, Unit, Int>(0),
    CombinedAbstractStatementVisitor<NothingVisitor, Int, Unit>,
    VoidStatementVisitor<NothingVisitor, Int> {

    override fun enterScope() {
        context += 1
    }

    override fun leave(expr: LiteralNode): Int {
        return 1
    }

    override fun leave(expr: ReadNode): Int {
        return 1
    }

    override fun leave(expr: OperatorApplicationNode, arguments: List<Int>): Int {
        return 1 + arguments.sum()
    }

    override fun leave(expr: QueryNode, arguments: List<Int>): Int {
        return 1 + arguments.sum()
    }

    override fun leave(expr: DeclassificationNode, expression: Int): Int {
        return 1 + expression
    }

    override fun leave(expr: EndorsementNode, expression: Int): Int {
        return 1 + expression
    }

    private fun checkValue(value: Int) {
        if (context < value) throw Error("Expression deeper than context!")
    }

    override fun leave(stmt: LetNode, value: Int) {
        checkValue(value)
    }

    override fun leave(stmt: DeclarationNode, arguments: List<Int>) {
        checkValue(arguments.sum())
    }

    override fun leave(stmt: UpdateNode, arguments: List<Int>) {
        checkValue(arguments.sum())
    }

    override fun leave(stmt: IfNode, guard: Int) {
        checkValue(guard)
    }

    override fun leave(stmt: OutputNode, message: Int) {
        checkValue(message)
    }

    override fun leave(stmt: SendNode, message: Int) {
        checkValue(message)
    }
}
