package io.github.aplcornell.viaduct.analysis

import io.github.aplcornell.viaduct.attributes.Tree
import io.github.aplcornell.viaduct.attributes.TreeNode
import io.github.aplcornell.viaduct.attributes.attribute
import mu.KotlinLogging
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

private val logger = KotlinLogging.logger("AnalysisProvider")

/**
 * Constructs and caches [Analysis] instances for a [RootNode] so that
 * the same instance can be retrieved at multiple places in the compiler.
 * This avoids recomputing the same information over and over again.
 *
 * This class uses reflection to construct [Analysis] instances.
 * Each [Analysis] class must have a primary constructor. The constructor
 * may have parameters of type: [TreeNode], [RootNode], and subclasses of [Analysis].
 * Sensible arguments will be passed to the constructor based on parameter types.
 */
class AnalysisProvider<Node : TreeNode<Node>, RootNode : Node>(rootNode: RootNode) {
    /** A lazily constructed [Tree] instance for the root node. */
    val tree: Tree<Node, RootNode> by lazy { Tree(rootNode) }

    val KClass<Analysis<RootNode>>.instance: Analysis<RootNode> by attribute {
        logger.debug("Constructing instance for $this.")
        val constructor = this.primaryConstructor!!
        val arguments =
            constructor.parameters.map {
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
        (A::class as KClass<Analysis<RootNode>>).instance
            as A
}
