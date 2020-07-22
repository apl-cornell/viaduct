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

fun Node.postorderTraverse(f: (Node) -> Unit) {
    this.children.forEach(f)
    f(this)
}

inline fun <reified T : Node> Node.listOfInstances(): List<T> {
    val result = mutableListOf<T>()
    this.postorderTraverse {
        if (it is T) {
            result.add(it)
        }
    }
    return result
}

fun Node.letNodes(): List<LetNode> = this.listOfInstances()
fun Node.declarationNodes(): List<DeclarationNode> = this.listOfInstances()
fun Node.infiniteLoopNodes(): List<InfiniteLoopNode> = this.listOfInstances()
fun Node.breakNodes(): List<BreakNode> = this.listOfInstances()
fun Node.queryNodes(): List<QueryNode> = this.listOfInstances()
fun Node.updateNodes(): List<UpdateNode> = this.listOfInstances()

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
