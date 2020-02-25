package edu.cornell.cs.apl.viaduct.syntax.intermediate.visitors

import edu.cornell.cs.apl.viaduct.syntax.intermediate.AssertionNode
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
import edu.cornell.cs.apl.viaduct.syntax.intermediate.QueryNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReadNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReceiveNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SendNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.StatementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode

/**
 * Uniformly computes a value from an expression by recursively reducing the expression.
 *
 * More concretely, consider the tree representation of an expressions.
 * Leaf nodes are assigned the value [initial], and other nodes are assigned the result of pairwise
 * applying [combine] to [initial] followed by all their children in left-to-right order.
 * The final result is the value assigned to the root node.
 *
 * Note that the final result will contain an instance of [initial] for each node in the expression,
 * including those that have children. This makes defining certain reducers, like the one for
 * computing size, easier.
 *
 * @sample ProgramSize
 */
interface ExpressionReducer<Result> : ExpressionVisitor<Result> {
    /** The value assigned to nodes that have no children, and included in every node's result. */
    val initial: Result

    /** The function used to combine the values of a node's children. */
    val combine: (Result, Result) -> Result

    override fun leave(node: LiteralNode): Result =
        initial

    override fun leave(node: ReadNode): Result =
        initial

    override fun leave(node: OperatorApplicationNode, arguments: List<Result>): Result =
        arguments.fold(initial, combine)

    override fun leave(node: QueryNode, arguments: List<Result>): Result =
        arguments.fold(initial, combine)

    override fun leave(node: DeclassificationNode, expression: Result): Result =
        combine(initial, expression)

    override fun leave(node: EndorsementNode, expression: Result): Result =
        combine(initial, expression)
}

/** Same as [ExpressionReducer] but for [StatementNode]s. */
interface StatementReducer<Result> : ExpressionReducer<Result>, StatementVisitor<Result, Result> {
    override fun leave(node: LetNode, value: Result): Result =
        combine(initial, value)

    override fun leave(node: DeclarationNode, arguments: List<Result>): Result =
        arguments.fold(initial, combine)

    override fun leave(node: UpdateNode, arguments: List<Result>): Result =
        arguments.fold(initial, combine)

    override fun leave(
        node: IfNode,
        guard: Result,
        thenBranch: Result,
        elseBranch: Result
    ): Result =
        listOf(guard, thenBranch, elseBranch).fold(initial, combine)

    override fun leave(node: InfiniteLoopNode, body: Result): Result =
        combine(initial, body)

    override fun leave(node: BreakNode): Result =
        initial

    override fun leave(node: AssertionNode, condition: Result): Result =
        combine(initial, condition)

    override fun leave(node: BlockNode, statements: List<Result>): Result =
        statements.fold(initial, combine)

    override fun leave(node: InputNode): Result =
        initial

    override fun leave(node: OutputNode, message: Result): Result =
        combine(initial, message)

    override fun leave(node: ReceiveNode): Result =
        initial

    override fun leave(node: SendNode, message: Result): Result =
        combine(initial, message)
}

/** Same as [ExpressionReducer] but for [ProgramNode]s. */
interface ProgramReducer<Result> : StatementReducer<Result>,
    ProgramVisitor<Result, Result, Result> {
    override fun leave(node: HostDeclarationNode): Result =
        initial

    override fun leave(
        node: ProcessDeclarationNode,
        body: SuspendedTraversal<Result, *, *, *, Unit, Unit>
    ): Result =
        combine(initial, body(this))

    override fun leave(node: ProgramNode, declarations: List<Result>): Result =
        declarations.fold(initial, combine)
}

/** Counts the number of nodes in an abstract syntax tree. */
object ProgramSize : ProgramReducer<Int> {
    override val initial: Int
        get() = 1

    override val combine: (Int, Int) -> Int
        get() = Int::plus
}
