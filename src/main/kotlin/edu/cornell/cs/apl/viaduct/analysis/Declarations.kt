package edu.cornell.cs.apl.viaduct.analysis

import edu.cornell.cs.apl.attributes.Tree
import edu.cornell.cs.apl.viaduct.protocols.MainProtocol
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BreakNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InfiniteLoopNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.Node
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProcessDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.QueryNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode

/** Returns all [LetNode]s in this node. */
fun Node.letNodes(): List<LetNode> {
    val result = mutableListOf<LetNode>()
    fun traverse(node: Node) {
        if (node is LetNode) {
            result.add(node)
        }
        node.children.forEach(::traverse)
    }
    traverse(this)
    return result
}

/** Returns all [DeclarationNode]s in this node. */
fun Node.declarationNodes(): List<DeclarationNode> {
    val result = mutableListOf<DeclarationNode>()
    fun traverse(node: Node) {
        if (node is DeclarationNode) {
            result.add(node)
        }
        node.children.forEach(::traverse)
    }
    traverse(this)
    return result
}

fun Node.infiniteLoopNodes(): List<InfiniteLoopNode> {
    val result = mutableListOf<InfiniteLoopNode>()
    fun traverse(node: Node) {
        if (node is InfiniteLoopNode) {
            result.add(node)
        }
        node.children.forEach(::traverse)
    }
    traverse(this)
    return result
}

fun Node.breakNodes(): List<BreakNode> {
    val result = mutableListOf<BreakNode>()
    fun traverse(node: Node) {
        if (node is BreakNode) {
            result.add(node)
        }
        node.children.forEach(::traverse)
    }
    traverse(this)
    return result
}

fun Node.queryNodes(): List<QueryNode> {
    val result = mutableListOf<QueryNode>()
    fun traverse(node: Node) {
        if (node is QueryNode) {
            result.add(node)
        }
        node.children.forEach(::traverse)
    }
    traverse(this)
    return result
}

fun Node.updateNodes(): List<UpdateNode> {
    val result = mutableListOf<UpdateNode>()
    fun traverse(node: Node) {
        if (node is UpdateNode) {
            result.add(node)
        }
        node.children.forEach(::traverse)
    }
    traverse(this)
    return result
}

// TODO merge into nameanalysis?
fun InfiniteLoopNode.correspondingBreaks(): List<BreakNode> {
    return this.breakNodes().filter { it.jumpLabel == this.jumpLabel }
}

fun Node.involvedLoops(tree: Tree<Node, ProgramNode>): List<InfiniteLoopNode> {
    val result = mutableListOf<InfiniteLoopNode>()
    fun traverse(node: Node) {
        if (node is InfiniteLoopNode) {
            result.add(node)
        }
        when (val parent = tree.parent(node)) {
            null -> return
            else -> traverse(parent)
        }
    }
    traverse(this)
    return result
}

fun DeclarationNode.uses(tree: Tree<Node, ProgramNode>): Set<Node> {
    return (tree.root.queryNodes().filter { it.variable == this.variable } +
            tree.root.updateNodes().filter { it.variable == this.variable }).toSet()
}

/**
 * Returns the declaration of the [MainProtocol] in this program.
 */
// TODO: throws blah blah
val ProgramNode.main: ProcessDeclarationNode
    get() {
        this.forEach {
            if (it is ProcessDeclarationNode && it.protocol.value == MainProtocol)
                return it
        }
        // TODO: custom error message
        error("No main")
    }
