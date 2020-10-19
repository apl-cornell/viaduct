package edu.cornell.cs.apl.viaduct.analysis

import edu.cornell.cs.apl.attributes.attribute
import edu.cornell.cs.apl.attributes.circularAttribute
import edu.cornell.cs.apl.viaduct.errors.IllegalInternalCommunicationError
import edu.cornell.cs.apl.viaduct.errors.NoProtocolAnnotationError
import edu.cornell.cs.apl.viaduct.errors.UnknownObjectDeclarationError
import edu.cornell.cs.apl.viaduct.protocols.Local
import edu.cornell.cs.apl.viaduct.selection.CommunicationEvent
import edu.cornell.cs.apl.viaduct.selection.ProtocolComposer
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AssertionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BlockNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BreakNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DowngradeNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExpressionArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.FunctionArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.FunctionCallNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.FunctionDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.IfNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InfiniteLoopNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InternalCommunicationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LiteralNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.Node
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ObjectDeclarationArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutParameterInitializationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ParameterNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProcessDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReadNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReceiveNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SendNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SimpleStatementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.StatementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentHashSetOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet

/** Associates each [StatementNode] with the [Protocol]s involved in its execution. */
class ProtocolAnalysis(
    val program: ProgramNode,
    private val protocolComposer: ProtocolComposer
) {
    private val tree = program.tree
    private val nameAnalysis = NameAnalysis.get(program)

    /** The outermost block this [Node] is in. */
    private val Node.enclosingBody: BlockNode by attribute {
        when (val parent = tree.parent(this)!!) {
            is ProcessDeclarationNode ->
                parent.body

            is FunctionDeclarationNode ->
                parent.body

            else ->
                parent.enclosingBody
        }
    }

    /**
     * Returns the protocol that coordinates the execution of [statement].
     *
     * @throws IllegalInternalCommunicationError if [statement] is an [InternalCommunicationNode].
     */
    fun primaryProtocol(statement: SimpleStatementNode): Protocol {
        return when (statement) {
            is LetNode -> {
                val protocol =
                    statement.protocol?.value ?: throw NoProtocolAnnotationError(statement)

                when (statement.value) {
                    is InputNode ->
                        assert(protocol == Local(statement.value.host.value))
                    is ReceiveNode ->
                        throw IllegalInternalCommunicationError(statement.value)
                    else ->
                        Unit
                }

                protocol
            }

            is DeclarationNode ->
                statement.protocol?.value ?: throw NoProtocolAnnotationError(statement)

            is UpdateNode ->
                when (val decl = nameAnalysis.declaration(statement).declarationAsNode) {
                    is DeclarationNode ->
                        primaryProtocol(decl)

                    is ParameterNode ->
                        primaryProtocol(decl)

                    is ObjectDeclarationArgumentNode ->
                        primaryProtocol(nameAnalysis.parameter(decl))

                    else -> throw UnknownObjectDeclarationError(decl)
                }

            is OutParameterInitializationNode ->
                primaryProtocol(nameAnalysis.declaration(statement))

            is OutputNode ->
                Local(statement.host.value)

            is SendNode ->
                throw IllegalInternalCommunicationError(statement)
        }
    }

    /**
     * Returns the protocol that coordinates the execution of the let node read by [read].
     */
    fun primaryProtocol(read: ReadNode): Protocol = primaryProtocol(nameAnalysis.declaration(read))

    /**
     * Returns the protocol that coordinates the execution of [parameter].
     */
    fun primaryProtocol(parameter: ParameterNode): Protocol =
        parameter.protocol?.value ?: throw NoProtocolAnnotationError(parameter)

    /**
     * Returns the protocol that coordinates the execution of [argument].
     */
    fun primaryProtocol(argument: FunctionArgumentNode): Protocol =
        primaryProtocol(nameAnalysis.parameter(argument))

    /**
     * The [primaryProtocol]s of [SimpleStatementNode]s that read the temporary defined by this
     * statement.
     *
     * We define this as a separate attribute so the (potentially expensive) computation is cached.
     */
    private val LetNode.directReaders: PersistentSet<Protocol> by attribute {
        nameAnalysis.readers(this)
            .filterIsInstance<SimpleStatementNode>()
            .map { primaryProtocol(it) }
            .toPersistentSet()
    }

    private val FunctionDeclarationNode.protocols: PersistentSet<Protocol> by circularAttribute(
        persistentHashSetOf()
    ) {
        this.parameters
            .fold(persistentSetOf<Protocol>()) { acc, param ->
                acc.add(primaryProtocol(param))
            }
            .addAll(this.body.protocols)
            .addAll(
                nameAnalysis.calls(this).fold(persistentSetOf<Protocol>()) { acc, call ->
                    acc.addAll(call.protocols)
                }
            )
    }

    /** Used to compute [protocols]. */
    private val StatementNode.protocols: PersistentSet<Protocol> by circularAttribute(
        persistentHashSetOf()
    ) {
        when (this) {
            is LetNode -> {
                val indirectReaders =
                    nameAnalysis.readers(this)
                        .filter { it !is SimpleStatementNode && it !is FunctionCallNode }
                        .map { it.protocols }.unions()
                directReaders.addAll(indirectReaders).add(primaryProtocol(this))
            }

            is SimpleStatementNode ->
                persistentHashSetOf(primaryProtocol(this))

            // All protocols execute function calls;
            // also need to add primary protocols for arguments
            // and all protocols participating in the function body
            is FunctionCallNode ->
                nameAnalysis.declaration(this).protocols
                    .addAll(this.enclosingBody.protocols)

            is IfNode ->
                thenBranch.protocols.addAll(elseBranch.protocols)
            is InfiniteLoopNode ->
                body.protocols
            is BreakNode ->
                // Every protocol executing the loop executes the breaks in the loop.
                nameAnalysis.correspondingLoop(this).protocols
            is AssertionNode ->
                // All protocols execute every assertion.
                this.enclosingBody.protocols

            is BlockNode ->
                statements.map { it.protocols }.unions()
        }
    }

    /** Returns the set of protocols that direct read the let binding. */
    fun directReaders(letNode: LetNode): Set<Protocol> = letNode.directReaders

    /** Returns the set of protocols that execute [statement]. */
    fun protocols(statement: StatementNode): Set<Protocol> = statement.protocols

    /** Returns the set of protocols that execute [function]. */
    fun protocols(function: FunctionDeclarationNode): Set<Protocol> = function.protocols

    private val LetNode.relevantCommunicationEventsMap: Map<SimpleStatementNode, Set<CommunicationEvent>> by attribute {
        nameAnalysis
            .readers(this)
            .filterIsInstance<SimpleStatementNode>()
            .map { reader ->
                val readerHosts = reader.participatingHosts

                val protocol = primaryProtocol(this)
                val readerProtocol = primaryProtocol(reader)
                val phase = protocolComposer.communicate(protocol, readerProtocol)

                // relevance criterion: if (A) the receiver of the event is the reader protocol,
                // then (B) the event's receiving host must be participating
                // the implication (A) -> (B) is turned into !(A) || (B)
                val relevantEvents =
                    phase.filter { event ->
                        readerProtocol != event.recv.protocol || readerHosts.contains(event.recv.host)
                    }.toSet()

                Pair(reader, relevantEvents)
            }.toMap()
    }

    /** Return the relevant communication events for the [read]. */
    fun relevantCommunicationEvents(read: ReadNode): Set<CommunicationEvent> {
        val letNode = nameAnalysis.declaration(read)
        val reader = nameAnalysis.enclosingStatement(read) as SimpleStatementNode
        return letNode.relevantCommunicationEventsMap[reader]!!
    }

    /** Return the relevant communication events for [reader] reading from [letNode]. */
    fun relevantCommunicationEvents(letNode: LetNode, reader: SimpleStatementNode): Set<CommunicationEvent> {
        return letNode.relevantCommunicationEventsMap[reader]!!
    }

    private val StatementNode.participatingHosts: Set<Host> by circularAttribute(
        persistentHashSetOf()
    ) {
        when (this) {
            is DeclarationNode ->
                protocolComposer.mandatoryParticipatingHosts(primaryProtocol(this), this)

            is UpdateNode ->
                protocolComposer.mandatoryParticipatingHosts(primaryProtocol(this), this)

            is OutputNode ->
                protocolComposer.mandatoryParticipatingHosts(primaryProtocol(this), this)

            is OutParameterInitializationNode ->
                protocolComposer.mandatoryParticipatingHosts(primaryProtocol(this), this)

            is LetNode -> {
                val protocol = primaryProtocol(this)
                nameAnalysis.readers(this)
                    .map { reader ->
                        when (reader) {
                            is SimpleStatementNode -> {
                                val relevantEvents: Set<CommunicationEvent> =
                                    this.relevantCommunicationEventsMap[reader]!!

                                protocol.hosts.filter { host ->
                                    relevantEvents.any { event -> event.send.host == host }
                                }
                            }

                            is FunctionCallNode -> {
                                protocol.hosts.intersect(
                                    reader.arguments
                                        .filterIsInstance<ExpressionArgumentNode>()
                                        .filter { arg ->
                                            when (val argExpr = arg.expression) {
                                                is ReadNode -> argExpr.temporary.value == this.temporary.value
                                                is LiteralNode -> false
                                            }
                                        }.flatMap { arg ->
                                            primaryProtocol(nameAnalysis.parameter(arg)).hosts
                                        }
                                )
                            }

                            is IfNode -> {
                                protocol.hosts.intersect(reader.participatingHosts)
                            }

                            else -> setOf()
                        }

                    }.fold(
                        protocolComposer.mandatoryParticipatingHosts(protocol, this)
                    ) { acc, hostSet -> acc.union(hostSet) }
            }

            // TODO: what is the correct answer for this?
            is FunctionCallNode -> {
                nameAnalysis
                    .declaration(this).body.participatingHosts
                    .plus(
                        this.arguments.flatMap { arg ->
                            primaryProtocol(nameAnalysis.parameter(arg)).hosts
                        }
                    )
            }

            is InfiniteLoopNode ->
                this.body.participatingHosts

            is IfNode ->
                this.thenBranch.participatingHosts.union(this.elseBranch.participatingHosts)

            is BlockNode ->
                this.statements.fold(setOf()) { acc, stmt -> acc.union(stmt.participatingHosts) }

            else -> setOf()
        }
    }

    /** Returns the set of hosts that participate in the execution of [statement]. */
    fun participatingHosts(statement: StatementNode): Set<Host> = statement.participatingHosts

    /** Hosts that need to be synchronized after the statement. */
    private val StatementNode.hostsRequiringSync: Set<Host> by circularAttribute(
        persistentHashSetOf()
    ) {
        when (this) {
            is LetNode -> {
                when (this.value) {
                    is DowngradeNode -> program.hosts.map { it.name.value }.toSet()

                    else -> setOf()
                }
            }

            is BlockNode -> {
                this.statements.fold(setOf()) { acc, child -> acc.plus(child.hostsRequiringSync) }
            }

            is IfNode -> this.thenBranch.hostsRequiringSync.plus(this.elseBranch.hostsRequiringSync)

            is InfiniteLoopNode -> this.body.hostsRequiringSync

            else -> setOf()
        }
    }

    /** Hosts that will actually be synchronized after the statement. */
    private val StatementNode.hostsToSync: Set<Host> by attribute {
        when (this) {
            is LetNode, is IfNode, is InfiniteLoopNode, is BlockNode -> {
                this.hostsRequiringSync
                    .subtract(this.participatingHosts)
                    .intersect(nameAnalysis.enclosingBlock(this).participatingHosts)
            }

            else -> setOf()
        }
    }

    /** Returns the set of hosts that will synchronize with [statement]. */
    fun hostsToSync(statement: StatementNode): Set<Host> = statement.hostsToSync

    private val Node.participatingProtocols: Set<Protocol> by attribute {
        when (this) {
            is ProgramNode ->
                this.declarations.fold(setOf()) { acc, decl -> acc.plus(decl.participatingProtocols) }

            is ParameterNode ->
                this.protocol?.value?.let { setOf(it) } ?: throw NoProtocolAnnotationError(this)

            is FunctionDeclarationNode ->
                this.parameters
                    .fold<ParameterNode, Set<Protocol>>(setOf()) { acc, param -> acc.plus(param.participatingProtocols) }
                    .plus(this.body.participatingProtocols)

            is ProcessDeclarationNode ->
                this.body.participatingProtocols

            is BlockNode ->
                this.statements.fold(setOf()) { acc, child -> acc.plus(child.participatingProtocols) }

            is IfNode ->
                this.thenBranch.participatingProtocols.plus(this.elseBranch.participatingProtocols)

            is InfiniteLoopNode ->
                this.body.participatingProtocols

            is LetNode ->
                this.protocol?.value?.let { setOf(it) } ?: throw NoProtocolAnnotationError(this)

            is DeclarationNode ->
                this.protocol?.value?.let { setOf(it) } ?: throw NoProtocolAnnotationError(this)

            else -> setOf()
        }
    }

    /** Return the protocols participating for [node]. */
    fun participatingProtocols(node: Node): Set<Protocol> = node.participatingProtocols
}

/** Returns the union of all sets in this collection. */
private fun <E> Iterable<PersistentSet<E>>.unions(): PersistentSet<E> =
    this.fold(persistentHashSetOf()) { acc, set -> acc.addAll(set) }
