package edu.cornell.cs.apl.viaduct.analysis

import edu.cornell.cs.apl.viaduct.errors.NoMainError
import edu.cornell.cs.apl.viaduct.protocols.MainProtocol
import edu.cornell.cs.apl.viaduct.syntax.Located
import edu.cornell.cs.apl.viaduct.syntax.Name
import edu.cornell.cs.apl.viaduct.syntax.Variable
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BreakNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InfiniteLoopNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.Node
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ParameterNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProcessDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.QueryNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode
import edu.cornell.cs.apl.viaduct.util.FreshNameGenerator

/** Recursively traverses the children of [this] node, then applies [f] to [this] node. */
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

/** Returns all [ParameterNode]s contained in this node. */
fun Node.parameterNodes(): List<ParameterNode> = this.listOfInstances()

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
