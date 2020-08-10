package edu.cornell.cs.apl.viaduct.analysis

import edu.cornell.cs.apl.attributes.Tree
import edu.cornell.cs.apl.attributes.attribute
import edu.cornell.cs.apl.viaduct.errors.OutParameterInitializationError
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariable
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BlockNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.FunctionCallNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.FunctionDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.IfNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InfiniteLoopNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.Node
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutParameterArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutParameterInitializationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ParameterNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.QueryNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf

/** Analysis to ensure all out parameters have been initialized before
 * they are used and before the function returns. */
class OutParameterInitializationAnalysis private constructor(
    private val tree: Tree<Node, ProgramNode>,
    private val nameAnalysis: NameAnalysis
) {
    /** Defines initialization map of out parameters BEFORE node has been executed. */
    private val Node.flowIn: PersistentMap<ObjectVariable, Boolean> by attribute {
        val parent = tree.parent(this)
        val previousSibling = tree.previousSibling(this)
        when {
            parent == null -> {
                require(this is ProgramNode)
                persistentMapOf()
            }

            parent is BlockNode && previousSibling != null -> {
                previousSibling.flowOut
            }

            parent is FunctionDeclarationNode -> parent.flowOut

            else -> parent.flowIn
        }
    }

    /** Take the meet of two maps. */
    private fun mapMeet(
        map1: PersistentMap<ObjectVariable, Boolean>,
        map2: PersistentMap<ObjectVariable, Boolean>
    ): PersistentMap<ObjectVariable, Boolean> {
        assert(map1.keys == map2.keys)
        return map1.keys.fold(persistentMapOf()) { acc, key -> acc.put(key, map1[key]!! && map2[key]!!) }
    }

    /** Defines initialization map of out parameters AFTER node has been processed. */
    private val Node.flowOut: PersistentMap<ObjectVariable, Boolean> by attribute {
        when (this) {
            is FunctionDeclarationNode -> {
                parameters
                    .map { param -> Pair(param.name.value, param.isInParameter) }
                    .fold(persistentMapOf()) { acc, pair -> acc.put(pair.first, pair.second) }
            }

            is OutParameterInitializationNode -> {
                this.flowIn.put(this.name.value, true)
            }

            is FunctionCallNode -> {
                arguments
                    .filterIsInstance<OutParameterArgumentNode>()
                    .map { param -> param.parameter.value }
                    .fold(this.flowIn) { acc, param -> acc.put(param, true) }
            }

            // unify flowOuts from branches by taking the intersection of their
            // initialized out params
            is IfNode -> {
                mapMeet(thenBranch.flowOut, elseBranch.flowOut)
            }

            // disallow out param initialization inside of loops
            // note that this doesn't actually compute the fixpoint of the
            // dataflow equations as needed, because that needs a CircularAttribute.
            // TODO: handle breaks properly.
            is InfiniteLoopNode -> {
                mapMeet(this.flowIn, this.body.flowOut)
            }

            is BlockNode -> {
                this.children.last().flowOut
            }

            else -> this.flowIn
        }
    }

    /**
     * Check if all out parameters have been initialized before
     * they are used and before the function returns.
     */
    private fun check(node: Node) {
        when (node) {
            is FunctionDeclarationNode -> {
                for (kv in node.body.flowOut) {
                    if (!kv.value) {
                        throw OutParameterInitializationError(node.getParameter(kv.key)!!)
                    }
                }
            }

            is UpdateNode -> {
                val initialized = node.flowIn[node.variable.value] ?: true
                if (!initialized) {
                    throw OutParameterInitializationError(
                        nameAnalysis.declaration(node) as ParameterNode,
                        node
                    )
                }
            }

            is QueryNode -> {
                val initialized = node.flowIn[node.variable.value] ?: true
                if (!initialized) {
                    throw OutParameterInitializationError(
                        nameAnalysis.declaration(node) as ParameterNode,
                        node
                    )
                }
            }
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
        private val ProgramNode.instance: OutParameterInitializationAnalysis
            by attribute { OutParameterInitializationAnalysis(this.tree, NameAnalysis.get(this)) }

        override fun get(program: ProgramNode): OutParameterInitializationAnalysis = program.instance
    }
}
