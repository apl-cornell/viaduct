package edu.cornell.cs.apl.viaduct.analysis

import edu.cornell.cs.apl.viaduct.errors.IncorrectNumberOfArgumentsError
import edu.cornell.cs.apl.viaduct.errors.NoMainError
import edu.cornell.cs.apl.viaduct.syntax.FunctionName
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
import edu.cornell.cs.apl.viaduct.syntax.intermediate.FunctionDeclarationNode
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
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.QueryNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReadNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.StatementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode
import edu.cornell.cs.apl.viaduct.util.FreshNameGenerator

fun StatementNode.createdVariables(): List<Variable> =
    when (this) {
        is LetNode -> listOf(this.temporary.value)
        is DeclarationNode -> listOf(this.name.value)
        is UpdateNode -> listOf()
        is OutParameterInitializationNode -> listOf(this.name.value) // TODO is this right?
        is OutputNode -> listOf()
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

fun StatementNode.immediateRHS(): List<ExpressionNode> {
    return when (this) {
        is LetNode -> listOf(this.value)
        is DeclarationNode -> this.arguments
        is UpdateNode -> this.arguments
        is OutputNode -> listOf(this.message)
        is IfNode -> listOf(this.guard)
        is AssertionNode -> listOf(this.condition)
        else -> listOf()
    }
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
    }
}

/** Returns [this] node and all its descendants in post order. */
fun Node.descendants(): Sequence<Node> =
    sequence {
        this@descendants.children.forEach { yieldAll(it.descendants()) }
        yield(this@descendants)
    }

/** Returns all instances of [T] contained in [this] node (which may include [this] node). */
inline fun <reified T : Node> Node.descendantsIsInstance(): Sequence<T> =
    this.descendants().filterIsInstance<T>()

/** Name of the "main" function. */
val mainFunction = FunctionName("main")

/**
 * Returns the declaration of [mainFunction] function in this program.
 *
 * @throws NoMainError if the program has no such declaration.
 * @throws IncorrectNumberOfArgumentsError if the main function has any parameters.
 */
val ProgramNode.main: FunctionDeclarationNode
    get() =
        this.functions.find { it.name.value == mainFunction }?.also {
            if (it.parameters.isNotEmpty())
                throw IncorrectNumberOfArgumentsError(it.name, 0, it.parameters)
        } ?: throw NoMainError(this.sourceLocation.sourcePath)

/** A [FreshNameGenerator] that will avoid all [Variable] names in this node. */
fun Node.freshVariableNameGenerator(): FreshNameGenerator {
    val freshNameGenerator = FreshNameGenerator()

    fun <Named> Sequence<Named>.addNames(getName: Named.() -> Located<Name>) {
        this.forEach { freshNameGenerator.getFreshName(it.getName().value.name) }
    }
    this.descendantsIsInstance<LetNode>().addNames(LetNode::temporary)
    this.descendantsIsInstance<DeclarationNode>().addNames(DeclarationNode::name)
    this.descendantsIsInstance<ParameterNode>().addNames(ParameterNode::name)

    return freshNameGenerator
}
