package io.github.aplcornell.viaduct.analysis

import io.github.aplcornell.viaduct.attributes.Tree
import io.github.aplcornell.viaduct.attributes.TreeNode
import io.github.aplcornell.viaduct.attributes.attribute
import mu.KotlinLogging
import kotlin.reflect.KClass

private val logger = KotlinLogging.logger("AnalysisProvider")

class AnalysisProvider<Node : TreeNode<Node>, RootNode : Node>(rootNode: RootNode) {
    /** A lazily constructed [Tree] instance for the root node. */
    val tree: Tree<Node, RootNode> by lazy { Tree(rootNode) }

    val KClass<Analysis<RootNode>>.instance: Analysis<RootNode> by attribute {
        logger.debug("Constructing instance for $this")
        val constructor = this.constructors.first()
        val arguments = constructor.parameters.map {
            when (it.type.classifier) {
                rootNode::class -> {
                    rootNode
                }

                tree::class -> {
                    tree
                }

                else -> {
                    @Suppress("UNCHECKED_CAST")
                    (it.type.classifier as KClass<Analysis<RootNode>>).instance
                }
            }
        }
        constructor.call(*arguments.toTypedArray())
    }

    /**
     * Returns the [Analysis] instance for the root node.
     * The returned instance is cached for efficiency, so calling [get] again will return the same instance.
     */
    inline fun <reified A : Analysis<RootNode>> get(): A =
        @Suppress("UNCHECKED_CAST")
        (A::class as KClass<Analysis<RootNode>>).instance as A
}
