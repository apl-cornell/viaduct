package io.github.aplcornell.viaduct.analysis

import io.github.aplcornell.viaduct.attributes.Tree
import io.github.aplcornell.viaduct.attributes.attribute
import io.github.aplcornell.viaduct.errors.OutParameterInitializationError
import io.github.aplcornell.viaduct.syntax.ObjectVariable
import io.github.aplcornell.viaduct.syntax.intermediate.BlockNode
import io.github.aplcornell.viaduct.syntax.intermediate.FunctionCallNode
import io.github.aplcornell.viaduct.syntax.intermediate.FunctionDeclarationNode
import io.github.aplcornell.viaduct.syntax.intermediate.IfNode
import io.github.aplcornell.viaduct.syntax.intermediate.InfiniteLoopNode
import io.github.aplcornell.viaduct.syntax.intermediate.Node
import io.github.aplcornell.viaduct.syntax.intermediate.ObjectReferenceArgumentNode
import io.github.aplcornell.viaduct.syntax.intermediate.ObjectVariableDeclarationNode
import io.github.aplcornell.viaduct.syntax.intermediate.OutParameterArgumentNode
import io.github.aplcornell.viaduct.syntax.intermediate.OutParameterInitializationNode
import io.github.aplcornell.viaduct.syntax.intermediate.ParameterNode
import io.github.aplcornell.viaduct.syntax.intermediate.ProgramNode
import io.github.aplcornell.viaduct.syntax.intermediate.QueryNode
import io.github.aplcornell.viaduct.syntax.intermediate.UpdateNode
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentMap

private enum class InitializationState {
    UNINITIALIZED, INITIALIZED, UNKNOWN;

    fun meet(that: InitializationState): InitializationState =
        if (this == that) this else UNKNOWN

    fun join(that: InitializationState): InitializationState =
        when {
            this == UNINITIALIZED && that == UNINITIALIZED ->
                UNINITIALIZED

            this == UNKNOWN || that == UNKNOWN ->
                UNKNOWN

            else -> INITIALIZED
        }
}

/** Take the meet of two maps. */
private fun meet(
    map1: PersistentMap<ObjectVariable, InitializationState>,
    map2: PersistentMap<ObjectVariable, InitializationState>
): PersistentMap<ObjectVariable, InitializationState> {
    require(map1.keys == map2.keys)
    return map1.keys.fold(persistentMapOf()) { acc, key ->
        acc.put(key, map1.getValue(key).meet(map2.getValue(key)))
    }
}

/**
 * Analysis that ensures all out parameters of functions are initialized before
 * they are used and before the function returns.
 */
class OutParameterInitializationAnalysis private constructor(
    private val tree: Tree<Node, ProgramNode>,
    private val nameAnalysis: NameAnalysis
) {
    /** Defines initialization map of out parameters BEFORE node has been executed. */
    private val Node.flowIn: PersistentMap<ObjectVariable, InitializationState> by attribute {
        val parent = tree.parent(this)
        val previousSibling = tree.previousSibling(this)
        when {
            parent == null -> {
                require(this is ProgramNode)
                persistentMapOf()
            }

            parent is BlockNode && previousSibling != null ->
                previousSibling.flowOut

            parent is FunctionDeclarationNode -> parent.flowOut

            else -> parent.flowIn
        }
    }

    /** Defines initialization map of out parameters AFTER node has been processed. */
    private val Node.flowOut: PersistentMap<ObjectVariable, InitializationState> by attribute {
        when (this) {
            is FunctionDeclarationNode ->
                parameters
                    .filter { param -> !param.isInParameter }
                    .associate { param -> param.name.value to InitializationState.UNINITIALIZED }
                    .toPersistentMap()

            is OutParameterInitializationNode ->
                this.flowIn.put(
                    this.name.value,
                    this.flowIn.getValue(this.name.value).join(InitializationState.INITIALIZED)
                )

            is FunctionCallNode ->
                arguments
                    .filterIsInstance<OutParameterArgumentNode>()
                    .map { param -> param.parameter.value }
                    .fold(this.flowIn) { acc, param ->
                        acc.put(param, this.flowIn.getValue(param).join(InitializationState.INITIALIZED))
                    }

            // Unify output flows from both branches.
            is IfNode ->
                meet(thenBranch.flowOut, elseBranch.flowOut)

            // Disallow out param initialization inside of loops.
            // Note that this doesn't actually compute the fixpoint of the
            // dataflow equations as needed, because that needs a CircularAttribute.
            // TODO: handle breaks properly.
            is InfiniteLoopNode ->
                meet(this.flowIn, this.body.flowOut)

            is BlockNode ->
                if (this.children.any()) {
                    this.children.last().flowOut
                } else {
                    this.flowIn
                }

            else -> this.flowIn
        }
    }

    /**
     * Check if all out parameters have been initialized before
     * they are used and before the function returns.
     */
    private fun check(node: Node) {
        fun use(declaration: ObjectVariableDeclarationNode) {
            node.flowIn[declaration.name.value]?.let {
                if (it != InitializationState.INITIALIZED) {
                    throw OutParameterInitializationError(declaration as ParameterNode, node)
                }
            }
        }

        fun define(declaration: ParameterNode) {
            node.flowIn[declaration.name.value]?.let {
                if (it != InitializationState.UNINITIALIZED) {
                    throw OutParameterInitializationError(declaration, node)
                }
            }
        }

        when (node) {
            is FunctionDeclarationNode -> {
                for (kv in node.body.flowOut) {
                    if (kv.value != InitializationState.INITIALIZED) {
                        throw OutParameterInitializationError(node.getParameter(kv.key)!!)
                    }
                }
            }

            is UpdateNode ->
                use(nameAnalysis.declaration(node))

            is QueryNode ->
                use(nameAnalysis.declaration(node))

            is ObjectReferenceArgumentNode ->
                use(nameAnalysis.declaration(node))

            is OutParameterInitializationNode ->
                define(nameAnalysis.declaration(node))
        }

        for (child in node.children) {
            check(child)
        }
    }

    /** Begin check at ProgramNode [tree]. */
    fun check() {
        check(tree.root)
    }

    companion object : AnalysisProvider<OutParameterInitializationAnalysis> {
        private fun construct(program: ProgramNode) =
            OutParameterInitializationAnalysis(program.tree, NameAnalysis.get(program))

        override fun get(program: ProgramNode): OutParameterInitializationAnalysis =
            program.cached(::construct)
    }
}
