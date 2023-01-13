package io.github.aplcornell.viaduct.attributes

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import java.util.IdentityHashMap

/**
 * Computes and stores parent/child/sibling relations in a tree structure.
 *
 * This class is a simplified version of Kiama's tree relations.
 * Refer to the sections on
 * [Attribute Grammars](https://inkytonik.github.io/kiama/Attribution#tree-relations)
 * and
 * [Relations](https://inkytonik.github.io/kiama/Relations).
 * in Kiama's user manual for more information.
 *
 * @param Node The type of nodes in the tree.
 * @param RootNode The type of the root node of the tree.
 * @param root The root node of the tree.
 */
class Tree<Node : TreeNode<Node>, out RootNode : Node>(val root: RootNode) {
    private val relations: MutableMap<Node, NodeRelations<Node>> = IdentityHashMap()

    init {
        fun addNode(parent: Node?, node: Node, nodeIndex: Int) {
            val children = node.children.toPersistentList()
            if (relations.put(node, NodeRelations(parent, children, nodeIndex)) != null) {
                // TODO: custom exception class
                error("Duplicate child node $node.")
            }
            children.forEachIndexed { index, child -> addNode(node, child, index) }
        }

        addNode(null, root, 0)
    }

    /** Returns the parent of [node], or `null` if [node] is the root. */
    fun parent(node: Node): Node? = relations[node]!!.parent

    /** Returns the index of [node] in its parent's children list. */
    fun childIndex(node: Node): Int = relations[node]!!.index

    /**
     * Returns the node that occurs just before [node] in its parent's children list, or `null`
     * if [node] is the root or [node] is its parent's first child.
     */
    fun previousSibling(node: Node): Node? {
        val nodeInfo = relations[node]!!
        return nodeInfo.parent?.let { parent ->
            relations[parent]!!.children.getOrNull(nodeInfo.index - 1)
        }
    }

    /**
     * Returns the node that occurs just after [node] in its parent's children list, or `null`
     * if [node] is the root or [node] is its parent's last child.
     */
    fun nextSibling(node: Node): Node? {
        val nodeInfo = relations[node]!!
        return nodeInfo.parent?.let { parent ->
            relations[parent]!!.children.getOrNull(nodeInfo.index + 1)
        }
    }
}

/** A node in a [Tree]. */
interface TreeNode<out Node> {
    /** The list of all children nodes. This is empty for leaf nodes. */
    // TODO: this should be a list
    val children: Iterable<Node>
}

/**
 * Stores ancestry information about a node.
 *
 * @param parent The parent of this node, or `null` if this is the root node.
 * @param children The list of all children nodes.
 * @param index The index of this node in [parent]'s children list.
 */
private data class NodeRelations<Node>(
    val parent: Node?,
    val children: PersistentList<Node>,
    val index: Int,
)
