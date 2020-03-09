package edu.cornell.cs.apl.viaduct.analysis

import edu.cornell.cs.apl.attributes.Tree
import edu.cornell.cs.apl.attributes.attribute
import edu.cornell.cs.apl.attributes.collectedAttribute
import edu.cornell.cs.apl.viaduct.errors.NameClashError
import edu.cornell.cs.apl.viaduct.errors.UndefinedNameError
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
import kotlinx.collections.immutable.persistentSetOf

/**
 * Associates each use of a [Name] with its declaration, and every [Name] declaration with the
 * set of its uses.
 *
 * For example, [Temporary] variables are associated with [LetNode]s, [ObjectVariable]s with
 * [DeclarationNode]s, and [JumpLabel]s with [InfiniteLoopNode]s.
 * */
class NameAnalysis(val tree: Tree<Node, ProgramNode>) {
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

    /** Jump labels in scope for this node. */
    private val Node.loops: NameMap<JumpLabel, InfiniteLoopNode> by attribute {
        when (val parent = tree.parent(this)) {
            null ->
                NameMap()
            is InfiniteLoopNode ->
                parent.loops.put(parent.jumpLabel, parent)
            else ->
                parent.loops
        }
    }

    /** Same as [readers]. */
    private val Node.readers: Set<StatementNode> by collectedAttribute(tree) { node ->
        if (node is StatementNode) {
            node.reads.map { Pair(declaration(it), node) }
        } else {
            listOf()
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
            val previousSibling = tree.previousSibling(this)
            when {
                parent == null ->
                    NameMap()
                parent is BlockNode && previousSibling != null ->
                    previousSibling.contextOut
                parent is BlockNode && previousSibling == null && resetAtBlock ->
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
    fun loop(node: BreakNode): InfiniteLoopNode =
        node.loops[node.jumpLabel]

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

    // TODO: readers for other types of [Name]s

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
                is ReadNode ->
                    declaration(node)
                is QueryNode ->
                    declaration(node)
                is UpdateNode ->
                    declaration(node)
                is BreakNode ->
                    loop(node)
                is ExternalCommunicationNode ->
                    declaration(node)
                is InternalCommunicationNode ->
                    declaration(node)
            }
            // Check that there are no name clashes
            when (node) {
                is LetNode ->
                    node.temporaryDefinitions.put(node.temporary, node)
                is DeclarationNode ->
                    node.objectDeclarations.put(node.variable, node)
                is InfiniteLoopNode ->
                    node.loops.put(node.jumpLabel, node)
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
}
