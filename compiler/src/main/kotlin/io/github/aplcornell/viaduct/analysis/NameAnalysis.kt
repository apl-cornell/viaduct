package io.github.aplcornell.viaduct.analysis

import io.github.aplcornell.viaduct.attributes.Tree
import io.github.aplcornell.viaduct.attributes.attribute
import io.github.aplcornell.viaduct.attributes.circularAttribute
import io.github.aplcornell.viaduct.attributes.collectedAttribute
import io.github.aplcornell.viaduct.errors.IncorrectNumberOfArgumentsError
import io.github.aplcornell.viaduct.errors.NameClashError
import io.github.aplcornell.viaduct.errors.UndefinedNameError
import io.github.aplcornell.viaduct.security.LabelAnd
import io.github.aplcornell.viaduct.security.LabelConfidentiality
import io.github.aplcornell.viaduct.security.LabelExpression
import io.github.aplcornell.viaduct.security.LabelIntegrity
import io.github.aplcornell.viaduct.security.LabelJoin
import io.github.aplcornell.viaduct.security.LabelLiteral
import io.github.aplcornell.viaduct.security.LabelMeet
import io.github.aplcornell.viaduct.security.LabelOr
import io.github.aplcornell.viaduct.security.LabelParameter
import io.github.aplcornell.viaduct.selection.FunctionVariable
import io.github.aplcornell.viaduct.syntax.FunctionName
import io.github.aplcornell.viaduct.syntax.Host
import io.github.aplcornell.viaduct.syntax.JumpLabel
import io.github.aplcornell.viaduct.syntax.LabelNode
import io.github.aplcornell.viaduct.syntax.LabelVariable
import io.github.aplcornell.viaduct.syntax.Located
import io.github.aplcornell.viaduct.syntax.Name
import io.github.aplcornell.viaduct.syntax.NameMap
import io.github.aplcornell.viaduct.syntax.ObjectTypeNode
import io.github.aplcornell.viaduct.syntax.ObjectVariable
import io.github.aplcornell.viaduct.syntax.ProtocolNode
import io.github.aplcornell.viaduct.syntax.SourceLocation
import io.github.aplcornell.viaduct.syntax.Temporary
import io.github.aplcornell.viaduct.syntax.intermediate.BlockNode
import io.github.aplcornell.viaduct.syntax.intermediate.BreakNode
import io.github.aplcornell.viaduct.syntax.intermediate.CommunicationNode
import io.github.aplcornell.viaduct.syntax.intermediate.DeclarationNode
import io.github.aplcornell.viaduct.syntax.intermediate.DeclassificationNode
import io.github.aplcornell.viaduct.syntax.intermediate.EndorsementNode
import io.github.aplcornell.viaduct.syntax.intermediate.ExpressionNode
import io.github.aplcornell.viaduct.syntax.intermediate.FunctionArgumentNode
import io.github.aplcornell.viaduct.syntax.intermediate.FunctionCallNode
import io.github.aplcornell.viaduct.syntax.intermediate.FunctionDeclarationNode
import io.github.aplcornell.viaduct.syntax.intermediate.HostDeclarationNode
import io.github.aplcornell.viaduct.syntax.intermediate.IfNode
import io.github.aplcornell.viaduct.syntax.intermediate.InfiniteLoopNode
import io.github.aplcornell.viaduct.syntax.intermediate.LetNode
import io.github.aplcornell.viaduct.syntax.intermediate.Node
import io.github.aplcornell.viaduct.syntax.intermediate.ObjectDeclarationArgumentNode
import io.github.aplcornell.viaduct.syntax.intermediate.ObjectReferenceArgumentNode
import io.github.aplcornell.viaduct.syntax.intermediate.ObjectVariableDeclarationNode
import io.github.aplcornell.viaduct.syntax.intermediate.OutParameterArgumentNode
import io.github.aplcornell.viaduct.syntax.intermediate.OutParameterInitializationNode
import io.github.aplcornell.viaduct.syntax.intermediate.ParameterNode
import io.github.aplcornell.viaduct.syntax.intermediate.ProgramNode
import io.github.aplcornell.viaduct.syntax.intermediate.QueryNode
import io.github.aplcornell.viaduct.syntax.intermediate.ReadNode
import io.github.aplcornell.viaduct.syntax.intermediate.SimpleStatementNode
import io.github.aplcornell.viaduct.syntax.intermediate.StatementNode
import io.github.aplcornell.viaduct.syntax.intermediate.UpdateNode
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
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

    /** Temporary definitions in scope for this node. */
    private val Node.temporaryDefinitions: NameMap<Temporary, LetNode> by Context(true) {
        if (it is LetNode) listOf(Pair(it.name, it)) else listOf()
    }

    /** Object declarations in scope for this node. */
    private val Node.objectDeclarations: NameMap<ObjectVariable, ObjectVariableDeclarationNode> by Context(false) {
        when (it) {
            is DeclarationNode ->
                listOf(Pair(it.name, it))

            is FunctionDeclarationNode ->
                it.parameters.map { param -> Pair(param.name, param) }

            is FunctionCallNode ->
                it.arguments
                    .filterIsInstance<ObjectVariableDeclarationNode>()
                    .map { arg -> Pair(arg.name, arg) }

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

    /**
     * Threads a context through the program according to the scoping rules.
     *
     * @param resetAtBlock True if the context map should be cleared upon entering a block.
     * @param defines Returns the name defined by this node along with the context information
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
    fun declaration(node: QueryNode): ObjectVariableDeclarationNode =
        node.objectDeclarations[node.variable]

    /** Returns the statement that declares the [ObjectVariable] in [node]. */
    fun declaration(node: UpdateNode): ObjectVariableDeclarationNode =
        node.objectDeclarations[node.variable]

    /** Returns the loop that [node] is breaking out of. */
    fun correspondingLoop(node: BreakNode): InfiniteLoopNode =
        node.jumpTargets[node.jumpLabel]

    /** Returns the declaration of the [Host] in [node]. */
    fun declaration(node: CommunicationNode): HostDeclarationNode =
        (node as Node).hostDeclarations[node.host]

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
    fun declaration(node: ObjectReferenceArgumentNode): ObjectVariableDeclarationNode =
        node.objectDeclarations[node.variable]

    /** Returns the object referenced by the [node] function argument. */
    fun declaration(node: OutParameterArgumentNode): ParameterNode {
        val parameter = node.objectDeclarations[node.parameter]
        return when {
            parameter is ParameterNode && parameter.isOutParameter ->
                parameter

            else ->
                throw UndefinedNameError(parameter.name)
        }
    }

    /** Returns the object type declaration for [node]. */
    fun objectType(node: ObjectVariableDeclarationNode): ObjectTypeNode =
        when (node) {
            is ParameterNode ->
                node.objectType

            is DeclarationNode ->
                node.objectType

            is ObjectDeclarationArgumentNode ->
                parameter(node).objectType
        }

    /** Returns the function call that contains [argument]. */
    fun functionCall(argument: FunctionArgumentNode): FunctionCallNode =
        tree.parent(argument) as FunctionCallNode

    /** Returns the function declaration that contains [parameter]. */
    fun functionDeclaration(parameter: ParameterNode): FunctionDeclarationNode =
        tree.parent(parameter) as FunctionDeclarationNode

    /** Returns the parameter for which [node] is the argument. */
    fun parameter(node: FunctionArgumentNode): ParameterNode {
        val functionCall = functionCall(node)
        val argumentIndex = functionCall.arguments.indexOf(node)
        val functionDecl = declaration(functionCall)
        return functionDecl.getParameterAtIndex(argumentIndex)
            ?: throw IncorrectNumberOfArgumentsError(
                functionCall.name,
                functionDecl.parameters.size,
                functionCall.arguments
            )
    }

    /** The innermost function that contains this node.*/
    private val Node.enclosingFunction: FunctionDeclarationNode? by attribute {
        when (val parent = tree.parent(this)) {
            null -> null

            is FunctionDeclarationNode -> parent

            else -> parent.enclosingFunction
        }
    }

    /** Returns the function declaration enclosing [node]. */
    fun enclosingFunction(node: Node): FunctionDeclarationNode =
        node.enclosingFunction!!

    /** Returns label variable declarations in scope of [node]. */
    private fun Node.labelVariables(): Set<LabelVariable> =
        enclosingFunction(this).labelParameters.map { it.value }.toSet()

    /** Returns the function declaration enclosing [node]. */
    // TODO: this should just return the FunctionNode
    fun enclosingFunctionName(node: Node): FunctionName =
        node.enclosingFunction!!.name.value

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

    /** Returns all [ReadNode]s in this node. */
    // TODO: [Node.descendantsIsInstance] will give the same thing.
    fun reads(node: Node): Set<ReadNode> = node.reads

    private val ReadNode.enclosingStatement: StatementNode by attribute {
        when (val parent = tree.parent(this)) {
            is SimpleStatementNode -> parent
            is IfNode -> parent
            is ExpressionNode -> tree.parent(parent) as StatementNode
            else -> throw Error("read node not enclosed by a simple statement or conditional")
        }
    }

    /** Return the statement enclosing the [read] node. */
    fun enclosingStatement(read: ReadNode): StatementNode = read.enclosingStatement

    private val Node.queries: Set<QueryNode> by collectedAttribute(tree) { node ->
        if (node is QueryNode) {
            listOf(declaration(node) as Node to node)
        } else {
            listOf()
        }
    }

    private val Node.updates: Set<UpdateNode> by collectedAttribute(tree) { node ->
        if (node is UpdateNode) {
            listOf(declaration(node) as Node to node)
        } else {
            listOf()
        }
    }

    /** Returns the set of [QueryNode]s that reference the [ObjectVariable] declared by [node]. **/
    fun queriers(node: ObjectVariableDeclarationNode): Set<QueryNode> =
        (node as Node).queries

    /** Returns the set of [UpdateNode]s that reference the [ObjectVariable] declared by [node]. **/
    fun updaters(node: ObjectVariableDeclarationNode): Set<UpdateNode> =
        (node as Node).updates

    /** Returns the set of arguments where [Node] is used. */
    private val Node.argumentUses: Set<FunctionArgumentNode> by collectedAttribute(tree) { node ->
        when (node) {
            is ObjectReferenceArgumentNode ->
                listOf(declaration(node) as Node to node)

            is OutParameterArgumentNode ->
                listOf(declaration(node) as Node to node)

            else ->
                listOf()
        }
    }

    /** Returns the set of arguments where [node] is used. */
    fun argumentUses(node: ObjectVariableDeclarationNode): Set<FunctionArgumentNode> =
        (node as Node).argumentUses

    /** Returns the set of parameters for which [node] is used as an argument. */
    fun parameterUses(node: ObjectVariableDeclarationNode): Set<ParameterNode> =
        argumentUses(node).map { argument -> parameter(argument) }.toSet()

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

    fun correspondingLet(query: QueryNode): LetNode =
        (tree.parent(query) as LetNode)

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
                null -> persistentListOf()
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
            is FunctionDeclarationNode -> this as BlockNode
            else -> (parent as StatementNode).enclosingBlock
        }
    }

    /** Returns the block that encloses this statement. */
    fun enclosingBlock(stmt: StatementNode): BlockNode = stmt.enclosingBlock

    /** Returns the sites that call this function. */
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

    // TODO: this seems to be missing parameter and out argument nodes...
    private val StatementNode.variables: Set<FunctionVariable> by attribute {
        val functionName = enclosingFunctionName(this)
        this.descendantsIsInstance<DeclarationNode>().map { decl -> FunctionVariable(functionName, decl.name.value) }
            .plus(
                this.descendantsIsInstance<LetNode>()
                    .map { letNode -> FunctionVariable(functionName, letNode.name.value) }
            ).plus(
                this.descendantsIsInstance<UpdateNode>().map { update ->
                    when (val decl = declaration(update)) {
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
        fun ProtocolNode.check() {
            // All hosts in a protocol name must be declared.
            this.value.hosts.forEach { host ->
                (tree.root as Node).hostDeclarations[Located(host, this.sourceLocation)]
            }
        }

        fun LabelExpression.check(
            hosts: Set<Host>,
            labelVariables: Set<LabelVariable>,
            sourceLocation: SourceLocation
        ) {
            when (this) {
                is LabelLiteral -> {
                    if (name !in hosts) {
                        throw UndefinedNameError(Located(name, sourceLocation))
                    }
                }

                is LabelParameter -> {
                    if (name !in labelVariables) {
                        throw UndefinedNameError(Located(name, sourceLocation))
                    }
                }

                is LabelConfidentiality -> value.check(hosts, labelVariables, sourceLocation)
                is LabelIntegrity -> value.check(hosts, labelVariables, sourceLocation)
                is LabelJoin -> {
                    lhs.check(hosts, labelVariables, sourceLocation)
                    rhs.check(hosts, labelVariables, sourceLocation)
                }

                is LabelMeet -> {
                    lhs.check(hosts, labelVariables, sourceLocation)
                    rhs.check(hosts, labelVariables, sourceLocation)
                }

                is LabelAnd -> {
                    lhs.check(hosts, labelVariables, sourceLocation)
                    rhs.check(hosts, labelVariables, sourceLocation)
                }

                is LabelOr -> {
                    lhs.check(hosts, labelVariables, sourceLocation)
                    rhs.check(hosts, labelVariables, sourceLocation)
                }

                else -> {}
            }
        }

        /** check if a [LabelNode] has undeclared [Host] or [LabelVariable] */
        fun LabelNode.check(node: Node) {
            val hosts = node.hostDeclarations.keys
            val labelVariables = node.labelVariables()
            value.check(hosts, labelVariables, sourceLocation)
        }

        fun check(node: Node) {
            // Check that name references are valid
            when (node) {
                is ParameterNode ->
                    node.protocol?.check()

                is ReadNode ->
                    declaration(node)

                is QueryNode ->
                    declaration(node)

                is LetNode ->
                    node.protocol?.check()

                is DeclarationNode ->
                    node.protocol?.check()

                is UpdateNode ->
                    declaration(node)

                is OutParameterInitializationNode ->
                    declaration(node)

                is ObjectReferenceArgumentNode ->
                    declaration(node)

                is OutParameterArgumentNode ->
                    declaration(node)

                is FunctionCallNode ->
                    declaration(node)

                is BreakNode ->
                    correspondingLoop(node)

                is CommunicationNode ->
                    declaration(node)
            }
            // Check that there are no name clashes
            when (node) {
                is LetNode ->
                    node.temporaryDefinitions.put(node.name, node)

                is DeclarationNode ->
                    node.objectDeclarations.put(node.name, node)

                is InfiniteLoopNode ->
                    node.jumpTargets.put(node.jumpLabel, node)

                is ProgramNode -> {
                    // Forcing these thunks
                    node.hostDeclarations
                    node.functionDeclarations
                }

                is FunctionDeclarationNode -> {
                    // check for duplicate LabelVariable declaration
                    val nameMap = NameMap<LabelVariable, LabelVariable>()
                    node.labelParameters.forEach {
                        nameMap.put(it, it.value)
                    }
                }
            }
            // Check that LabelVariables and Hosts are declared
            when (node) {
                is DeclarationNode ->
                    node.objectType.labelArguments?.forEach { it.check(node) }

                is DeclassificationNode -> {
                    node.fromLabel?.check(node)
                    node.toLabel.check(node)
                }

                is EndorsementNode -> {
                    node.fromLabel.check(node)
                    node.toLabel?.check(node)
                }

                is ParameterNode -> {
                    node.objectType.labelArguments?.forEach { it.check(node) }
                }
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
