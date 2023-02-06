package io.github.apl_cornell.viaduct.circuitanalysis

import io.github.apl_cornell.viaduct.errors.IncorrectNumberOfArgumentsError
import io.github.apl_cornell.viaduct.errors.NoMainError
import io.github.apl_cornell.viaduct.syntax.FunctionName
import io.github.apl_cornell.viaduct.syntax.Located
import io.github.apl_cornell.viaduct.syntax.Name
import io.github.apl_cornell.viaduct.syntax.Variable
import io.github.apl_cornell.viaduct.syntax.intermediate.AssertionNode
import io.github.apl_cornell.viaduct.syntax.intermediate.BlockNode
import io.github.apl_cornell.viaduct.syntax.intermediate.BreakNode
import io.github.apl_cornell.viaduct.syntax.intermediate.DeclarationNode
import io.github.apl_cornell.viaduct.syntax.intermediate.DeclassificationNode
import io.github.apl_cornell.viaduct.syntax.intermediate.EndorsementNode
import io.github.apl_cornell.viaduct.syntax.intermediate.ExpressionNode
import io.github.apl_cornell.viaduct.syntax.intermediate.FunctionCallNode
import io.github.apl_cornell.viaduct.syntax.intermediate.FunctionDeclarationNode
import io.github.apl_cornell.viaduct.syntax.intermediate.IfNode
import io.github.apl_cornell.viaduct.syntax.intermediate.InfiniteLoopNode
import io.github.apl_cornell.viaduct.syntax.intermediate.InputNode
import io.github.apl_cornell.viaduct.syntax.intermediate.LetNode
import io.github.apl_cornell.viaduct.syntax.intermediate.LiteralNode
import io.github.apl_cornell.viaduct.syntax.intermediate.Node
import io.github.apl_cornell.viaduct.syntax.intermediate.ObjectDeclarationArgumentNode
import io.github.apl_cornell.viaduct.syntax.intermediate.OperatorApplicationNode
import io.github.apl_cornell.viaduct.syntax.intermediate.OutParameterInitializationNode
import io.github.apl_cornell.viaduct.syntax.intermediate.OutputNode
import io.github.apl_cornell.viaduct.syntax.intermediate.ParameterNode
import io.github.apl_cornell.viaduct.syntax.intermediate.ProgramNode
import io.github.apl_cornell.viaduct.syntax.intermediate.QueryNode
import io.github.apl_cornell.viaduct.syntax.intermediate.ReadNode
import io.github.apl_cornell.viaduct.syntax.intermediate.StatementNode
import io.github.apl_cornell.viaduct.syntax.intermediate.UpdateNode
import io.github.apl_cornell.viaduct.util.FreshNameGenerator

fun StatementNode.createdVariables(): List<Variable> =
    when (this) {
        is LetNode -> listOf(this.name.value)
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
    this.descendantsIsInstance<LetNode>().addNames(LetNode::name)
    this.descendantsIsInstance<DeclarationNode>().addNames(DeclarationNode::name)
    this.descendantsIsInstance<ParameterNode>().addNames(ParameterNode::name)

    return freshNameGenerator
}
