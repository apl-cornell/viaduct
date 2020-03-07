package edu.cornell.cs.apl.viaduct.analysis

import edu.cornell.cs.apl.viaduct.protocols.MainProtocol
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.Node
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProcessDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode

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
