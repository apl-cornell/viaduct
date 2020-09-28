package edu.cornell.cs.apl.viaduct.analysis

import edu.cornell.cs.apl.viaduct.errors.NoMainError
import edu.cornell.cs.apl.viaduct.protocols.MainProtocol
import edu.cornell.cs.apl.viaduct.syntax.Located
import edu.cornell.cs.apl.viaduct.syntax.Name
import edu.cornell.cs.apl.viaduct.syntax.Variable
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AssertionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BlockNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BreakNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclassificationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.EndorsementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.FunctionCallNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.IfNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InfiniteLoopNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LiteralNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.Node
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ObjectDeclarationArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OperatorApplicationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutParameterInitializationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ParameterNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProcessDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.QueryNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReadNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReceiveNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SendNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.StatementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode
import edu.cornell.cs.apl.viaduct.util.FreshNameGenerator

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

fun StatementNode.createdVariables(): List<Variable> =
    when (this) {
        is LetNode -> listOf(this.temporary.value)
        is DeclarationNode -> listOf(this.name.value)
        is UpdateNode -> listOf()
        is OutParameterInitializationNode -> listOf(this.name.value) // TODO is this right?
        is OutputNode -> listOf()
        is SendNode -> listOf()
        is FunctionCallNode ->
            this.arguments.filterIsInstance<ObjectDeclarationArgumentNode>().map {
                it.name.value
            } // TODO what about OutParameterArgumentNode?
        is IfNode -> listOf()
        is InfiniteLoopNode -> listOf()
        is BreakNode -> listOf()
        is AssertionNode -> listOf()
        is BlockNode -> listOf()
    }

fun ExpressionNode.involvedVariables(): List<Variable> {
    return when (this) {
        is ReadNode -> listOf(this.temporary.value)
        is LiteralNode -> listOf()
        is OperatorApplicationNode -> this.arguments.flatMap { it.involvedVariables() }
        is QueryNode -> listOf(this.variable.value) + this.arguments.flatMap { it.involvedVariables() }
        is DeclassificationNode -> this.expression.involvedVariables()
        is EndorsementNode -> this.expression.involvedVariables()
        is InputNode -> listOf()
        is ReceiveNode -> listOf()
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

/** Returns all [ObjectDeclarationArgumentNode]s contained in this node. */
fun Node.objectDeclarationArgumentNodes(): List<ObjectDeclarationArgumentNode> = this.listOfInstances()

/** Returns all [ParameterNode]s contained in this node. */
fun Node.parameterNodes(): List<ParameterNode> = this.listOfInstances()

/** Returns all [IfNode]s contained in this node. */
fun Node.ifNodes(): List<IfNode> = this.listOfInstances()

/** Returns all [InfiniteLoopNode]s contained in this node. */
fun Node.infiniteLoopNodes(): List<InfiniteLoopNode> = this.listOfInstances()

/** Returns all [BreakNode]s contained in this node. */
fun Node.breakNodes(): List<BreakNode> = this.listOfInstances()

/** Returns all [QueryNode]s contained in this node. */
fun Node.queryNodes(): List<QueryNode> = this.listOfInstances()

/** Returns all [UpdateNode]s contained in this node. */
fun Node.updateNodes(): List<UpdateNode> = this.listOfInstances()

/** Returns all [OutputNode]s contained in this node. */
fun Node.outputNodes(): List<OutputNode> = this.listOfInstances()

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

val ProgramNode.hasMain: Boolean
    get() {
        this.forEach {
            if (it is ProcessDeclarationNode && it.protocol.value == MainProtocol)
                return true
        }
        return false
    }

/** A [FreshNameGenerator] that will avoid all [Variable] names in this node. */
fun Node.freshVariableNameGenerator(): FreshNameGenerator {
    val freshNameGenerator = FreshNameGenerator()

    fun <Named> Iterable<Named>.addNames(getName: Named.() -> Located<Name>) {
        this.forEach { freshNameGenerator.getFreshName(it.getName().value.name) }
    }
    this.letNodes().addNames(LetNode::temporary)
    this.declarationNodes().addNames(DeclarationNode::name)
    this.parameterNodes().addNames(ParameterNode::name)

    return freshNameGenerator
}
