package io.github.aplcornell.viaduct.syntax.intermediate

import io.github.aplcornell.viaduct.attributes.Tree
import io.github.aplcornell.viaduct.attributes.TreeNode
import io.github.aplcornell.viaduct.prettyprinting.Document
import io.github.aplcornell.viaduct.prettyprinting.PrettyPrintable
import io.github.aplcornell.viaduct.syntax.HasSourceLocation
import io.github.aplcornell.viaduct.syntax.JumpLabel
import io.github.aplcornell.viaduct.syntax.Variable

/** Metadata information per node. */
typealias Metadata = Map<Node, PrettyPrintable>

/**
 * A node in the intermediate language abstract syntax tree.
 *
 * The intermediate language is similar to the surface language, but with the following changes:
 *
 * - For and while loops are elaborated into loop-until-break statements.
 * - Expressions are in A-normal form. Briefly, this means all intermediate results are stored
 *   in immutable temporary variables.
 * - Every loop and break statement has a [JumpLabel].
 * - All [Variable]s within a process have unique names.
 */
abstract class Node : TreeNode<Node>, HasSourceLocation, PrettyPrintable {
    /**
     * Returns a representation of this node in the surface syntax.
     *
     * This is useful, for example, for [pretty printing][PrettyPrintable].
     *
     * @param metadata Associates metadata with some nodes, which is converted into a comment.
     */
    abstract fun toSurfaceNode(metadata: Metadata = mapOf()): io.github.aplcornell.viaduct.syntax.surface.Node

    /**
     * Returns a shallow copy of this node where the child nodes are replaced by [children].
     *
     * This method can be used to generate objects with unique object identities, for example,
     * when constructing a [Tree] since [Tree] assumes there is no sharing.
     *
     * The returned node is guaranteed to have a new object identity even if [children] exactly matches the
     * children of this node, however, the nodes in [children] themselves are not copied.
     * This method assumes that [children] contains the correct number and types of nodes.
     */
    abstract fun copy(children: List<Node> = this.children.toList()): Node

    final override fun toDocument(): Document = toSurfaceNode(mapOf()).toDocument()

    /** Returns a pretty representation of this [Node] where each descendant is decorated using [metadata]. */
    fun toDocumentWithMetadata(metadata: Metadata): Document = toSurfaceNode(metadata).toDocument()

    /** Converts the metadata associated with this [Node] into a comment. */
    protected fun metadataAsComment(metadata: Metadata): String? = metadata[this]?.toDocument()?.print()
}

/** Like [Node.copy], but recursively copies all descendant nodes also.*/
fun Node.deepCopy(): Node = this.copy(this.children.toList().map { it.deepCopy() })
