package io.github.aplcornell.viaduct.precircuitanalysis

import io.github.aplcornell.viaduct.attributes.Tree
import io.github.aplcornell.viaduct.attributes.attribute
import io.github.aplcornell.viaduct.errors.NameClashError
import io.github.aplcornell.viaduct.errors.UndefinedNameError
import io.github.aplcornell.viaduct.syntax.FunctionName
import io.github.aplcornell.viaduct.syntax.Host
import io.github.aplcornell.viaduct.syntax.JumpLabel
import io.github.aplcornell.viaduct.syntax.Located
import io.github.aplcornell.viaduct.syntax.Name
import io.github.aplcornell.viaduct.syntax.NameMap
import io.github.aplcornell.viaduct.syntax.ObjectVariable
import io.github.aplcornell.viaduct.syntax.ProtocolNode
import io.github.aplcornell.viaduct.syntax.Temporary
import io.github.aplcornell.viaduct.syntax.precircuit.BlockNode
import io.github.aplcornell.viaduct.syntax.precircuit.CallNode
import io.github.aplcornell.viaduct.syntax.precircuit.ComputeLetNode
import io.github.aplcornell.viaduct.syntax.precircuit.FunctionDeclarationNode
import io.github.aplcornell.viaduct.syntax.precircuit.HostDeclarationNode
import io.github.aplcornell.viaduct.syntax.precircuit.LetNode
import io.github.aplcornell.viaduct.syntax.precircuit.LookupNode
import io.github.aplcornell.viaduct.syntax.precircuit.Node
import io.github.aplcornell.viaduct.syntax.precircuit.ParameterNode
import io.github.aplcornell.viaduct.syntax.precircuit.ProgramNode
import io.github.aplcornell.viaduct.syntax.precircuit.ReduceNode
import io.github.aplcornell.viaduct.syntax.precircuit.ReferenceNode
import io.github.aplcornell.viaduct.syntax.precircuit.Variable
import io.github.aplcornell.viaduct.syntax.precircuit.VariableBindingNode
import io.github.aplcornell.viaduct.syntax.precircuit.VariableDeclarationNode
import io.github.aplcornell.viaduct.syntax.precircuit.VariableNode
import io.github.aplcornell.viaduct.syntax.precircuit.VariableReferenceNode
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Associates each use of a [Name] with its declaration, and every [Name] declaration with the
 * set of its uses.
 *
 * For example, [Temporary] variables are associated with [LetNode]s, [ObjectVariable]s with
 * [DeclarationNode]s, and [JumpLabel]s with [InfiniteLoopNode]s.
 * */
class NameAnalysis private constructor(private val tree: Tree<Node, ProgramNode>) {
    /** Host declarations in scope for this node. */
    private val Node.hostDeclarations: NameMap<Host, HostDeclarationNode> by attribute {
        when (val parent = tree.parent(this)) {
            null -> {
                require(this is ProgramNode)
                declarations.filterIsInstance<HostDeclarationNode>()
                    .fold(NameMap()) { map, declaration -> map.put(declaration.name, declaration) }
            }

            else ->
                parent.hostDeclarations
        }
    }

    /** Function declarations in scope for this node. */
    private val Node.functionDeclarations: NameMap<FunctionName, FunctionDeclarationNode> by attribute {
        when (val parent = tree.parent(this)) {
            null -> {
                require(this is ProgramNode)
                declarations.filterIsInstance<FunctionDeclarationNode>()
                    .fold(NameMap()) { map, declaration ->
                        map.put(declaration.name, declaration)
                    }
            }

            else -> parent.functionDeclarations
        }
    }

    /** Variable declarations in scope for this node. */
    private val Node.variableDeclarations: NameMap<Variable, VariableDeclarationNode> by Context(
        ::definesAfter,
        ::definesChildren,
    )

    private fun definesAfter(node: Node): List<Pair<VariableNode, VariableDeclarationNode>> =
        when (node) {
            is ComputeLetNode -> listOf(Pair(node.name, node))

            is LetNode -> node.bindings.map { binding -> Pair(binding.name, binding) }

            else -> listOf()
        }

    private fun definesChildren(node: Node): List<Pair<VariableNode, VariableDeclarationNode>> =
        when (node) {
            is ReduceNode -> listOf(Pair(node.indices.name, node.indices))

            is ComputeLetNode -> node.indices.map { index -> Pair(index.name, index) }

            is FunctionDeclarationNode ->
                node.sizes.map { param -> Pair(param.name, param) } +
                    node.inputs.map { param -> Pair(param.name, param) }

            else -> listOf()
        }

    /**
     * Threads a context through the program according to the scoping rules.
     *
     * @param defines Returns the name defined by this node along with the context information
     *   attached to that name, or `null` if this node does not define a new name.
     */
    private inner class Context<N : Name, Data>(
        definesAfter: (Node) -> List<Pair<Located<N>, Data>>,
        definesChildren: (Node) -> List<Pair<Located<N>, Data>>,
    ) : ReadOnlyProperty<Node, NameMap<N, Data>> {
        /** Context just before this node. */
        private val Node.contextIn: NameMap<N, Data> by attribute {
            val parent = tree.parent(this)
            val previousSibling = tree.previousSibling(this)
            when {
                parent == null ->
                    NameMap()

                parent is BlockNode<*> && previousSibling != null ->
                    previousSibling.contextOut

                else ->
                    definesChildren(parent).fold(parent.contextIn) { acc, pair -> acc.put(pair.first, pair.second) }
            }
        }

        /** Context just after this node. */
        private val Node.contextOut: NameMap<N, Data> by attribute {
            definesAfter(this).fold(contextIn) { acc, pair -> acc.put(pair.first, pair.second) }
        }

        override fun getValue(thisRef: Node, property: KProperty<*>): NameMap<N, Data> =
            thisRef.contextIn
    }

    /** Returns the statement that defines the [Variable] in [node]. */
    fun declaration(node: VariableReferenceNode): VariableDeclarationNode =
        (node as Node).variableDeclarations[node.name]

    fun declaration(node: CallNode): FunctionDeclarationNode =
        node.functionDeclarations[node.name]

    /** Returns the funtion declaration that contains [parameter]. */
    fun functionDeclaration(parameter: ParameterNode): FunctionDeclarationNode =
        tree.parent(parameter) as FunctionDeclarationNode

    /** The innermost function that contains this node.*/
    private val Node.enclosingFunction: FunctionDeclarationNode? by attribute {
        when (val parent = tree.parent(this)) {
            null -> null

            is FunctionDeclarationNode -> parent

            else -> parent.enclosingFunction
        }
    }

    /** Returns the function declaration enclosing [node]. */
    fun enclosingFunction(node: Node): FunctionDeclarationNode? =
        node.enclosingFunction

    /**
     * Asserts that every referenced [Name] has a declaration, and that no [Name] is declared
     * multiple times in the same scope.
     *
     * @throws UndefinedNameError if a referenced [Name] is not in scope.
     * @throws NameClashError if a [Name] is declared multiple times in the same scope.
     */
    fun check() {
        fun ProtocolNode.check() {
            // All hosts in a protocol name must be declared.
            this.value.hosts.forEach { host ->
                (tree.root as Node).hostDeclarations[Located(host, this.sourceLocation)]
            }
        }

        fun check(node: Node) {
            // Check that name references are valid
            when (node) {
                is VariableBindingNode -> node.protocol.check()
                is ComputeLetNode -> node.protocol.check()
                is ReferenceNode -> declaration(node)
                is LookupNode -> declaration(node)
                is CallNode -> declaration(node)
                else -> {}
            }
            // Check that there are no name clashes
            when (node) {
                is ComputeLetNode -> {
                    node.variableDeclarations.put(node.name, node)
                    node.indices.forEach {
                        node.variableDeclarations.put(it.name, it)
                    }
                }

                is LetNode -> node.bindings.forEach { node.variableDeclarations.put(it.name, it) }
                is ReduceNode -> node.variableDeclarations.put(node.indices.name, node.indices)
                is ProgramNode -> {
                    // Forcing these thunks
                    node.hostDeclarations
                    node.functionDeclarations
                }

                else -> {} // TODO
            }
            // Check the children
            node.children.forEach(::check)
        }
        check(tree.root)
    }

    companion object : AnalysisProvider<NameAnalysis> {
        private fun construct(program: ProgramNode) = NameAnalysis(program.tree)

        override fun get(program: ProgramNode): NameAnalysis = program.cached(::construct)
    }
}
