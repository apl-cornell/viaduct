package edu.cornell.cs.apl.viaduct.analysis

import edu.cornell.cs.apl.attributes.Tree
import edu.cornell.cs.apl.attributes.attribute
import edu.cornell.cs.apl.attributes.collectedAttribute
import edu.cornell.cs.apl.viaduct.errors.NameClashError
import edu.cornell.cs.apl.viaduct.errors.UndefinedNameError
import edu.cornell.cs.apl.viaduct.protocols.Adversary
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.JumpLabel
import edu.cornell.cs.apl.viaduct.syntax.Located
import edu.cornell.cs.apl.viaduct.syntax.Name
import edu.cornell.cs.apl.viaduct.syntax.NameMap
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariable
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.Temporary
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BlockNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BreakNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExternalCommunicationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.HostDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.IfNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InfiniteLoopNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InternalCommunicationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.Node
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProcessDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.QueryNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReadNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.StatementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

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

    /** Protocol declarations in scope for this node. */
    private val Node.protocolDeclarations: NameMap<Protocol, ProcessDeclarationNode> by attribute {
        when (val parent = tree.parent(this)) {
            null -> {
                require(this is ProgramNode)
                declarations.filterIsInstance<ProcessDeclarationNode>()
                    .fold(NameMap()) { map, declaration ->
                        map.put(declaration.protocol, declaration)
                    }
            }
            else ->
                parent.protocolDeclarations
        }
    }

    /** Temporary definitions in scope for this node. */
    private val Node.temporaryDefinitions: NameMap<Temporary, LetNode> by Context(true) {
        if (it is LetNode) Pair(it.temporary, it) else null
    }

    /** Object declarations in scope for this node. */
    private val Node.objectDeclarations: NameMap<ObjectVariable, DeclarationNode> by Context(false) {
        if (it is DeclarationNode) Pair(it.variable, it) else null
    }

    /** Loop nodes in scope for this node. */
    private val Node.jumpTargets: NameMap<JumpLabel, InfiniteLoopNode> by attribute {
        when (val parent = tree.parent(this)) {
            null ->
                NameMap()
            is InfiniteLoopNode ->
                parent.jumpTargets.put(parent.jumpLabel, parent)
            else ->
                parent.jumpTargets
        }
    }

    /** The set of [Temporary] variables directly read by this node. */
    private val Node.reads: PersistentSet<ReadNode> by attribute {
        when (this) {
            is ReadNode ->
                persistentSetOf(this)
            is ExpressionNode ->
                children.fold(persistentSetOf()) { acc, child -> acc.addAll(child.reads) }
            else ->
                children.filterIsInstance<ExpressionNode>().fold(persistentSetOf()) { acc, child ->
                    acc.addAll(child.reads)
                }
        }
    }

    /**
     * Threads a context through the program according to the scoping rules.
     *
     * @param resetAtBlock True if the context map should be cleared upon entering a block.
     * @param defines Returns the name defined by this node along with the the context information
     *   attached to that name, or `null` if this node does not define a new name.
     */
    private inner class Context<N : Name, Data>(
        resetAtBlock: Boolean,
        defines: (Node) -> Pair<Located<N>, Data>?
    ) : ReadOnlyProperty<Node, NameMap<N, Data>> {
        /** Context just before this node. */
        private val Node.contextIn: NameMap<N, Data> by attribute {
            val parent = tree.parent(this)
            val grandParent = parent?.let { tree.parent(it) }
            val previousSibling = tree.previousSibling(this)
            when {
                parent == null ->
                    NameMap()
                parent is BlockNode && previousSibling != null ->
                    previousSibling.contextOut
                parent is BlockNode && previousSibling == null && grandParent !is BlockNode && resetAtBlock ->
                    // TODO: resetting at block is not enough to guarantee security with temporaries
                    NameMap()
                else ->
                    parent.contextIn
            }
        }

        /** Context just after this node. */
        private val Node.contextOut: NameMap<N, Data> by attribute {
            defines(this).let {
                if (it == null) contextIn else contextIn.put(it.first, it.second)
            }
        }

        override fun getValue(thisRef: Node, property: KProperty<*>): NameMap<N, Data> =
            thisRef.contextIn
    }

    /** Returns the statement that defines the [Temporary] in [node]. */
    fun declaration(node: ReadNode): LetNode =
        node.temporaryDefinitions[node.temporary]

    /** Returns the statement that declares the [ObjectVariable] in [node]. */
    fun declaration(node: QueryNode): DeclarationNode =
        node.objectDeclarations[node.variable]

    /** Returns the statement that declares the [ObjectVariable] in [node]. */
    fun declaration(node: UpdateNode): DeclarationNode =
        node.objectDeclarations[node.variable]

    /** Returns the loop that [node] is breaking out of. */
    fun correspondingLoop(node: BreakNode): InfiniteLoopNode =
        node.jumpTargets[node.jumpLabel]

    /** Returns the declaration of the [Host] in [node]. */
    fun declaration(node: ExternalCommunicationNode): HostDeclarationNode =
        (node as Node).hostDeclarations[node.host]

    /** Returns the declaration of the [Protocol] in [node]. */
    fun declaration(node: InternalCommunicationNode): ProcessDeclarationNode =
        (node as Node).protocolDeclarations[node.protocol]

    /**
     * Returns the set of [StatementNode]s that read the [Temporary] defined by [node].
     *
     * Note that this set only includes direct reads. For example, an [IfNode] only reads the
     * temporaries in its guard, and [BlockNode]s and [InfiniteLoopNode]s do not read any temporary.
     */
    fun readers(node: LetNode): Set<StatementNode> = node.readers

    private val Node.readers: Set<StatementNode> by collectedAttribute(tree) { node ->
        if (node is StatementNode) {
            node.reads.map { Pair(declaration(it), node) }
        } else {
            listOf()
        }
    }

    /** Returns the set of [QueryNode]s that reference the [ObjectVariable] declared by [node]. **/
    fun queriers(node: DeclarationNode): Set<QueryNode> = node.queries

    private val DeclarationNode.queries: Set<QueryNode> by collectedAttribute(tree) { node ->
        if (node is QueryNode) {
            listOf(declaration(node) to node)
        } else {
            listOf()
        }
    }

    /** Returns the set of [UpdateNode]s that reference the [ObjectVariable] declared by [node]. **/
    fun updaters(node: DeclarationNode): Set<UpdateNode> = node.updates

    private val DeclarationNode.updates: Set<UpdateNode> by collectedAttribute(tree) { node ->
        if (node is UpdateNode) {
            listOf(declaration(node) to node)
        } else {
            listOf()
        }
    }

    /** Returns the set of [QueryNode]s and [UpdateNode]s that reference the [ObjectVariable] declared by [node]. **/
    fun users(node: DeclarationNode): Set<Node> =
        queriers(node).union(updaters(node))

    /** Returns the set of [BreakNode]s that reference [node]. **/
    fun correspondingBreaks(node: InfiniteLoopNode): Set<BreakNode> = node.correspondingBreaks

    private val InfiniteLoopNode.correspondingBreaks: Set<BreakNode> by collectedAttribute(tree) { node ->
        if (node is BreakNode) {
            listOf(node.jumpTargets[node.jumpLabel] to node)
        } else {
            listOf()
        }
    }

    /** Returns the list of [InfiniteLoopNode]s [node] is contained in. **/
    fun involvedLoops(node: Node): List<InfiniteLoopNode> = node.involvedLoops

    private val Node.involvedLoops: List<InfiniteLoopNode> by attribute {
        val loopsAbove =
            when (val parent = tree.parent(this)) {
                null -> persistentListOf<InfiniteLoopNode>()
                else -> parent.involvedLoops
            }
        if (this is InfiniteLoopNode) {
            loopsAbove + persistentListOf(this)
        } else {
            loopsAbove
        }
    }

    /**
     * Asserts that every referenced [Name] has a declaration, and that no [Name] is declared
     * multiple times in the same scope.
     *
     * @throws UndefinedNameError if a referenced [Name] is not in scope.
     * @throws NameClashError if a [Name] is declared multiple times in the same scope.
     */
    fun check() {
        fun check(node: Node) {
            // Check that name references are valid
            when (node) {
                is ProcessDeclarationNode -> {
                    // All hosts in a protocol name must be declared.
                    node.protocol.value.hosts.forEach { host ->
                        node.hostDeclarations[Located(host, node.protocol.sourceLocation)]
                    }
                }
                is ReadNode ->
                    declaration(node)
                is QueryNode ->
                    declaration(node)
                is UpdateNode ->
                    declaration(node)
                is BreakNode ->
                    correspondingLoop(node)
                is ExternalCommunicationNode ->
                    declaration(node)
                is InternalCommunicationNode ->
                    // The adversary is always (implicitly) defined
                    if (node.protocol.value !is Adversary) declaration(node)
            }
            // Check that there are no name clashes
            when (node) {
                is LetNode ->
                    node.temporaryDefinitions.put(node.temporary, node)
                is DeclarationNode ->
                    node.objectDeclarations.put(node.variable, node)
                is InfiniteLoopNode ->
                    node.jumpTargets.put(node.jumpLabel, node)
                is ProgramNode -> {
                    // Forcing these thunks
                    node.hostDeclarations
                    node.protocolDeclarations
                }
            }
            // Check the children
            node.children.forEach(::check)
        }
        check(tree.root)
    }

    companion object : AnalysisProvider<NameAnalysis> {
        private val ProgramNode.instance: NameAnalysis by attribute { NameAnalysis(this.tree) }

        override fun get(program: ProgramNode): NameAnalysis = program.instance
    }
}
