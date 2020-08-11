package edu.cornell.cs.apl.viaduct.analysis

import edu.cornell.cs.apl.viaduct.errors.NoMainError
import edu.cornell.cs.apl.viaduct.protocols.MainProtocol
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AssertionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BreakNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.IfNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InfiniteLoopNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.Node
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProcessDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.QueryNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SendNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.StatementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode

/** Recursively traverses the children of [this] node, then applies [f] to [this] node. */
fun StatementNode.immediateRHS(): List<ExpressionNode> {
    return when (this) {
        is LetNode -> listOf(this.value)
        is DeclarationNode -> this.arguments
        is UpdateNode -> this.arguments
        is OutputNode -> listOf(this.message)
        is SendNode -> listOf(this.message)
        is IfNode -> listOf(this.guard)
        is AssertionNode -> listOf(this.condition)
        else -> listOf()
    }
}

private fun Node.postorderTraverse(f: (Node) -> Unit) {
    this.children.forEach { it.postorderTraverse(f) }
    f(this)
}

/** Returns all instances of [T] contained in [this] node. */
private inline fun <reified T : Node> Node.listOfInstances(): List<T> {
    val result = mutableListOf<T>()
    this.postorderTraverse {
        if (it is T) {
            result.add(it)
        }
    }
    return result
}

/** Returns all [LetNode]s contained in this node. */
fun Node.letNodes(): List<LetNode> = this.listOfInstances()

/** Returns all [DeclarationNode]s contained in this node. */
fun Node.declarationNodes(): List<DeclarationNode> = this.listOfInstances()

/** Returns all [InfiniteLoopNode]s contained in this node. */
fun Node.infiniteLoopNodes(): List<InfiniteLoopNode> = this.listOfInstances()

/** Returns all [BreakNode]s contained in this node. */
fun Node.breakNodes(): List<BreakNode> = this.listOfInstances()

/** Returns all [QueryNode]s contained in this node. */
fun Node.queryNodes(): List<QueryNode> = this.listOfInstances()

/** Returns all [UpdateNode]s contained in this node. */
fun Node.updateNodes(): List<UpdateNode> = this.listOfInstances()

/**
 * Returns the declaration of the [MainProtocol] in this program.
 *
 * @throws NoMainError if the program has no such declaration.
 */
val ProgramNode.main: ProcessDeclarationNode
    get() {
        this.forEach {
            if (it is ProcessDeclarationNode && it.protocol.value == MainProtocol)
                return it
        }
        throw NoMainError(this.sourceLocation.sourcePath)
    }
