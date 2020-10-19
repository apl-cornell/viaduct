package edu.cornell.cs.apl.viaduct.analysis

import edu.cornell.cs.apl.attributes.Tree
import edu.cornell.cs.apl.attributes.attribute
import edu.cornell.cs.apl.attributes.circularAttribute
import edu.cornell.cs.apl.attributes.collectedAttribute
import edu.cornell.cs.apl.viaduct.errors.IncorrectNumberOfArgumentsError
import edu.cornell.cs.apl.viaduct.errors.NameClashError
import edu.cornell.cs.apl.viaduct.errors.UndefinedNameError
import edu.cornell.cs.apl.viaduct.errors.UnknownObjectDeclarationError
import edu.cornell.cs.apl.viaduct.protocols.Adversary
import edu.cornell.cs.apl.viaduct.selection.FunctionVariable
import edu.cornell.cs.apl.viaduct.syntax.Arguments
import edu.cornell.cs.apl.viaduct.syntax.ClassNameNode
import edu.cornell.cs.apl.viaduct.syntax.FunctionName
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.JumpLabel
import edu.cornell.cs.apl.viaduct.syntax.LabelNode
import edu.cornell.cs.apl.viaduct.syntax.Located
import edu.cornell.cs.apl.viaduct.syntax.Name
import edu.cornell.cs.apl.viaduct.syntax.NameMap
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariable
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariableNode
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.Temporary
import edu.cornell.cs.apl.viaduct.syntax.ValueTypeNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BlockNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BreakNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExternalCommunicationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.FunctionArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.FunctionCallNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.FunctionDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.HostDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.IfNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InfiniteLoopNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InternalCommunicationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.Node
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ObjectDeclaration
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ObjectDeclarationArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ObjectReferenceArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutParameterArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutParameterInitializationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ParameterNode
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

    /** Temporary definitions in scope for this node. */
    private val Node.temporaryDefinitions: NameMap<Temporary, LetNode> by Context(true) {
        if (it is LetNode) listOf(Pair(it.temporary, it)) else listOf()
    }

    /** Object declarations in scope for this node. */
    private val Node.objectDeclarations: NameMap<ObjectVariable, ObjectDeclaration> by Context(false) {
        when (it) {
            is DeclarationNode ->
                listOf(Pair(it.name, it as ObjectDeclaration))

            is FunctionDeclarationNode ->
                it.parameters.map { param -> Pair(param.name, param as ObjectDeclaration) }

            is FunctionCallNode ->
                it.arguments
                    .filterIsInstance<ObjectDeclarationArgumentNode>()
                    .map { arg -> Pair(arg.name, asObjectDeclaration(arg)) }

            else -> listOf()
        }
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
            is OutParameterInitializationNode ->
                children.fold(persistentSetOf()) { acc, child -> acc.addAll(child.reads) }
            is FunctionCallNode ->
                children.fold(persistentSetOf()) { acc, child -> acc.addAll(child.reads) }
            else ->
                children.filterIsInstance<ExpressionNode>().fold(persistentSetOf()) { acc, child ->
                    acc.addAll(child.reads)
                }
        }
    }

    private val FunctionArgumentNode.functionCall: FunctionCallNode by attribute {
        tree.parent(this) as FunctionCallNode
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
        defines: (Node) -> List<Pair<Located<N>, Data>>
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
                parent is FunctionDeclarationNode ->
                    parent.contextOut
                else ->
                    parent.contextIn
            }
        }

        /** Context just after this node. */
        private val Node.contextOut: NameMap<N, Data> by attribute {
            defines(this).fold(contextIn) { acc, pair -> acc.put(pair.first, pair.second) }
        }

        override fun getValue(thisRef: Node, property: KProperty<*>): NameMap<N, Data> =
            thisRef.contextIn
    }

    /** Returns the statement that defines the [Temporary] in [node]. */
    fun declaration(node: ReadNode): LetNode =
        node.temporaryDefinitions[node.temporary]

    /** Returns the statement that declares the [ObjectVariable] in [node]. */
    fun declaration(node: QueryNode): ObjectDeclaration =
        node.objectDeclarations[node.variable]

    /** Returns the statement that declares the [ObjectVariable] in [node]. */
    fun declaration(node: UpdateNode): ObjectDeclaration =
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

    /** Returns the declaration of the out parameter in [node]. */
    fun declaration(node: OutParameterInitializationNode): ParameterNode {
        return when (val parameter = node.objectDeclarations[node.name]) {
            is ParameterNode -> parameter
            else -> throw UndefinedNameError(node.name)
        }
    }

    /** Returns the declaration of the function being called in [node]. */
    fun declaration(node: FunctionCallNode): FunctionDeclarationNode =
        (node as Node).functionDeclarations[node.name]

    /** Returns the object referenced by the [node] function argument. */
    fun declaration(node: ObjectReferenceArgumentNode): ObjectDeclaration =
        node.objectDeclarations[node.variable]

    /** Returns the object referenced by the [node] function argument. */
    fun declaration(node: OutParameterArgumentNode): ParameterNode {
        val parameter = node.objectDeclarations[node.parameter]
        return when {
            parameter is ParameterNode && parameter.isOutParameter -> parameter
            else -> throw UndefinedNameError(parameter.name)
        }
    }

    fun asObjectDeclaration(node: ObjectDeclarationArgumentNode): ObjectDeclaration {
        val parameter = parameter(node)
        return object : ObjectDeclaration {
            override val name: ObjectVariableNode
                get() = node.name

            override val className: ClassNameNode
                get() = parameter.className

            override val typeArguments: Arguments<ValueTypeNode>
                get() = parameter.typeArguments

            override val labelArguments: Arguments<LabelNode>?
                get() = parameter.labelArguments

            override val declarationAsNode: Node
                get() = node
        }
    }

    /** Returns the parameter for which [node] is the argument. */
    fun parameter(node: FunctionArgumentNode): ParameterNode {
        val functionCall = node.functionCall
        val argumentIndex = functionCall.arguments.indexOf(node)
        val functionDecl = declaration(functionCall)
        return functionDecl.getParameterAtIndex(argumentIndex)
            ?: throw IncorrectNumberOfArgumentsError(
                functionCall.name,
                functionDecl.parameters.size,
                functionCall.arguments
            )
    }

    /** Get the function declaration where the parameter is in. */
    private val ParameterNode.functionDeclaration: FunctionDeclarationNode by attribute {
        tree.root.functionDeclarations.values.first { f -> f.parameters.contains(this) }
    }

    fun functionDeclaration(parameter: ParameterNode): FunctionDeclarationNode = parameter.functionDeclaration

    /** Get the function declaration enclosing this node. */
    private val Node.enclosingFunction: FunctionDeclarationNode? by attribute {
        when (this) {
            is ProgramNode -> null

            is FunctionDeclarationNode -> this

            else -> tree.parent(this)!!.enclosingFunction
        }
    }

    fun enclosingFunctionName(node: StatementNode): FunctionName =
        node.enclosingFunction?.name?.value ?: MAIN_FUNCTION

    fun enclosingFunctionName(node: ExpressionNode): FunctionName =
        node.enclosingFunction?.name?.value ?: MAIN_FUNCTION

    fun enclosingFunctionName(node: FunctionArgumentNode): FunctionName =
        node.enclosingFunction?.name?.value ?: MAIN_FUNCTION

    private val Node.readers: Set<StatementNode> by collectedAttribute(tree) { node ->
        if (node is StatementNode) {
            node.reads.map { Pair(declaration(it), node) }
        } else {
            listOf()
        }
    }

    /**
     * Returns the set of [StatementNode]s that read the [Temporary] defined by [node].
     *
     * Note that this set only includes direct reads. For example, an [IfNode] only reads the
     * temporaries in its guard, and [BlockNode]s and [InfiniteLoopNode]s do not read any temporary.
     */
    fun readers(node: LetNode): Set<StatementNode> = node.readers

    fun reads(node: Node): Set<ReadNode> = node.reads

    private val Node.queries: Set<QueryNode> by collectedAttribute(tree) { node ->
        if (node is QueryNode) {
            listOf(declaration(node).declarationAsNode to node)
        } else {
            listOf()
        }
    }

    /** Returns the set of [QueryNode]s that reference the [ObjectVariable] declared by [node]. **/
    fun queriers(node: DeclarationNode): Set<QueryNode> = node.queries

    /** Returns the set of [QueryNode]s that reference the [ObjectVariable] declared by [node]. **/
    fun queriers(node: ParameterNode): Set<QueryNode> = node.queries

    /** Returns the set of [QueryNode]s that reference the [ObjectVariable] declared by [node]. **/
    fun queriers(node: ObjectDeclarationArgumentNode): Set<QueryNode> = node.queries

    private val ObjectDeclarationArgumentNode.queries: Set<QueryNode> by collectedAttribute(tree) { node ->
        if (node is QueryNode) {
            listOf(declaration(node).declarationAsNode to node)
        } else {
            listOf()
        }
    }

    private val Node.updates: Set<UpdateNode> by collectedAttribute(tree) { node ->
        if (node is UpdateNode) {
            listOf(declaration(node).declarationAsNode to node)
        } else {
            listOf()
        }
    }

    /** Returns the set of [UpdateNode]s that reference the [ObjectVariable] declared by [node]. **/
    fun updaters(node: DeclarationNode): Set<UpdateNode> = node.updates

    /** Returns the set of [UpdateNode]s that reference the [ObjectVariable] declared by [node]. **/
    fun updaters(node: ParameterNode): Set<UpdateNode> = node.updates

    /** Returns the set of [UpdateNode]s that reference the [ObjectVariable] declared by [node]. **/
    fun updaters(node: ObjectDeclarationArgumentNode): Set<UpdateNode> = node.updates

    /** Returns the set of [QueryNode]s and [UpdateNode]s that reference the [ObjectVariable] declared by [node]. **/
    fun users(node: DeclarationNode): Set<Node> =
        queriers(node).union(updaters(node))

    /** Returns the set of [QueryNode]s and [UpdateNode]s that reference the [ObjectVariable] declared by [node]. **/
    fun users(node: ParameterNode): Set<Node> =
        queriers(node).union(updaters(node))

    /** Returns the set of [QueryNode]s and [UpdateNode]s that reference the [ObjectVariable] declared by [node]. **/
    fun users(node: ObjectDeclarationArgumentNode): Set<Node> =
        queriers(node).union(updaters(node))

    /** Returns the set of arguments where [Node] is used. */
    private val Node.argumentUses: Set<FunctionArgumentNode> by collectedAttribute(tree) { node ->
        when (node) {
            is ObjectReferenceArgumentNode ->
                listOf(declaration(node).declarationAsNode to node)

            is OutParameterArgumentNode ->
                listOf(declaration(node).declarationAsNode to node)

            else ->
                listOf()
        }
    }

    /** Returns the set of arguments where [node] is used. */
    fun argumentUses(node: DeclarationNode): Set<FunctionArgumentNode> = node.argumentUses

    /** Returns the set of arguments where [node] is used. */
    fun argumentUses(node: ParameterNode): Set<FunctionArgumentNode> = node.argumentUses

    /** Returns the set of arguments where [node] is used. */
    fun argumentUses(node: ObjectDeclarationArgumentNode): Set<FunctionArgumentNode> = node.argumentUses

    /** Returns the set of parameters for which [node] is used as an argument. */
    fun parameterUses(node: DeclarationNode): Set<ParameterNode> =
        node.argumentUses.map { argument -> parameter(argument) }.toSet()

    /** Returns the set of parameters for which [node] is used as an argument. */
    fun parameterUses(node: ParameterNode): Set<ParameterNode> =
        node.argumentUses.map { argument -> parameter(argument) }.toSet()

    /** Returns the set of parameters for which [node] is used as an argument. */
    fun parameterUses(node: ObjectDeclarationArgumentNode): Set<ParameterNode> =
        node.argumentUses.map { argument -> parameter(argument) }.toSet()

    /** Returns the set of arguments for [ParameterNode]. */
    private val ParameterNode.parameterUsers: Set<FunctionArgumentNode> by collectedAttribute(tree) { node ->
        when (node) {
            is FunctionArgumentNode ->
                listOf(parameter(node) to node)

            else ->
                listOf()
        }
    }

    /** Returns the set of arguments for [ParameterNode]. */
    fun parameterUsers(parameter: ParameterNode): Set<FunctionArgumentNode> = parameter.parameterUsers

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

    private val StatementNode.enclosingBlock: BlockNode by attribute {
        when (val parent = tree.parent(this)) {
            is BlockNode -> parent
            is IfNode -> parent.enclosingBlock
            is InfiniteLoopNode -> parent.enclosingBlock
            is FunctionDeclarationNode -> this as BlockNode
            is ProcessDeclarationNode -> this as BlockNode
            else -> throw Error("statement parent has to be a block node!")
        }
    }

    /** Get the block that encloses this statement. */
    fun enclosingBlock(stmt: StatementNode): BlockNode = stmt.enclosingBlock

    /** Get the sites that call a function. */
    private val FunctionDeclarationNode.calls: Set<FunctionCallNode> by collectedAttribute(tree) { node ->
        when (node) {
            is FunctionCallNode -> listOf(declaration(node) to node)
            else -> listOf()
        }
    }

    /** Returns the set of calls to the given function declaration. */
    fun calls(function: FunctionDeclarationNode): Set<FunctionCallNode> = function.calls

    /** Set of functions reachable from a node by transitively following function calls. */
    private val Node.reachableFunctions: PersistentSet<FunctionName> by circularAttribute(
        persistentSetOf()
    ) {
        if (this is FunctionCallNode) {
            declaration(this).reachableFunctions.add(this.name.value)
        } else {
            this.children.fold(persistentSetOf()) { acc, child -> acc.addAll(child.reachableFunctions) }
        }
    }

    /** Returns the set of functions transitively reachable from a statement node. */
    fun reachableFunctions(node: Node) = node.reachableFunctions

    private val StatementNode.variables: Set<FunctionVariable> by attribute {
        val functionName = enclosingFunctionName(this)
        this.declarationNodes().map { decl -> FunctionVariable(functionName, decl.name.value) }
            .plus(
                this.letNodes().map { letNode -> FunctionVariable(functionName, letNode.temporary.value) }
            ).plus(
                this.updateNodes().map { update ->
                    when (val decl = declaration(update).declarationAsNode) {
                        is DeclarationNode ->
                            FunctionVariable(functionName, decl.name.value)

                        is ParameterNode ->
                            FunctionVariable(
                                functionDeclaration(decl).name.value,
                                decl.name.value
                            )

                        is ObjectDeclarationArgumentNode -> {
                            val param = parameter(decl)
                            FunctionVariable(
                                functionDeclaration(param).name.value,
                                decl.name.value
                            )
                        }

                        else -> throw UnknownObjectDeclarationError(this)
                    }
                }
            ).toSet()
    }

    /** Returns the variables used of a node. */
    fun variables(node: StatementNode): Set<FunctionVariable> = node.variables

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
                is OutParameterInitializationNode ->
                    declaration(node)
                is FunctionCallNode -> {
                    declaration(node)
                    for (argument in node.arguments) {
                        when (argument) {
                            is ObjectReferenceArgumentNode -> declaration(argument)
                            is OutParameterArgumentNode -> declaration(argument)
                        }
                    }
                }
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
                    node.objectDeclarations.put(node.name, node)
                is InfiniteLoopNode ->
                    node.jumpTargets.put(node.jumpLabel, node)
                is ProgramNode -> {
                    // Forcing these thunks
                    node.hostDeclarations
                    node.protocolDeclarations
                    node.functionDeclarations
                }
            }
            // Check the children
            node.children.forEach(::check)
        }
        check(tree.root)
    }

    companion object : AnalysisProvider<NameAnalysis> {
        private val ProgramNode.instance: NameAnalysis by attribute { NameAnalysis(this.tree) }

        val MAIN_FUNCTION = FunctionName("#main#")

        override fun get(program: ProgramNode): NameAnalysis = program.instance
    }
}
