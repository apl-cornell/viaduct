package edu.cornell.cs.apl.viaduct.analysis

import edu.cornell.cs.apl.attributes.attribute
import edu.cornell.cs.apl.attributes.circularAttribute
import edu.cornell.cs.apl.viaduct.errors.IllegalInternalCommunicationError
import edu.cornell.cs.apl.viaduct.protocols.Local
import edu.cornell.cs.apl.viaduct.syntax.FunctionName
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.Variable
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AssertionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BlockNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BreakNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DowngradeNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.FunctionCallNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.FunctionDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.IfNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InfiniteLoopNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InternalCommunicationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.Node
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutParameterInitializationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ParameterNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProcessDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
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
    val protocolAssignment: (FunctionName, Variable) -> Protocol
) {
    val tree = program.tree
    val nameAnalysis = NameAnalysis.get(program)

    /** The [ProcessDeclarationNode] this [Node] is in. */
    private val Node.enclosingBlock: BlockNode by attribute {
        when (val parent = tree.parent(this)!!) {
            is ProcessDeclarationNode ->
                parent.body

            is FunctionDeclarationNode ->
                parent.body

            else ->
                parent.enclosingBlock
        }
    }

    /**
     * Returns the protocol that coordinates the execution of [statement].
     *
     * @throws IllegalInternalCommunicationError if [statement] is an [InternalCommunicationNode].
     */
    fun primaryProtocol(statement: SimpleStatementNode): Protocol {
        val functionName = nameAnalysis.enclosingFunctionName(statement)
        return when (statement) {
            is LetNode -> {
                val protocol = protocolAssignment(functionName, statement.temporary.value)
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
                protocolAssignment(functionName, statement.name.value)
            is UpdateNode ->
                protocolAssignment(functionName, statement.variable.value)

            is OutParameterInitializationNode ->
                protocolAssignment(functionName, statement.name.value)

            is OutputNode ->
                Local(statement.host.value)
            is SendNode ->
                throw IllegalInternalCommunicationError(statement)
        }
    }

    /**
     * Returns the protocol that coordinates the execution of [parameter].
     */
    fun primaryProtocol(parameter: ParameterNode): Protocol =
        protocolAssignment(nameAnalysis.functionDeclaration(parameter).name.value, parameter.name.value)

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
                acc.add(protocolAssignment(this.name.value, param.name.value))
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
                    .addAll(this.enclosingBlock.protocols)

            is IfNode ->
                thenBranch.protocols.addAll(elseBranch.protocols)
            is InfiniteLoopNode ->
                body.protocols
            is BreakNode ->
                // Every protocol executing the loop executes the breaks in the loop.
                nameAnalysis.correspondingLoop(this).protocols
            is AssertionNode ->
                // All protocols execute every assertion.
                this.enclosingBlock.protocols

            is BlockNode ->
                statements.map { it.protocols }.unions()
        }
    }

    /** Returns the set of protocols that execute [statement]. */
    fun protocols(statement: StatementNode): Set<Protocol> = statement.protocols

    /** Returns the set of protocols that execute [function]. */
    fun protocols(function: FunctionDeclarationNode): Set<Protocol> = function.protocols

    /** Used to compute [syncProtocols]. */
    private val StatementNode.syncProtocols: Set<Protocol> by circularAttribute(
        persistentHashSetOf()
    ) {
        when (this) {
            is LetNode -> {
                when (this.value) {
                    is DowngradeNode -> this.enclosingBlock.protocols
                    else -> setOf()
                }
            }

            else -> setOf()
        }
    }

    /** Returns the set of protocols that must synchronize with [statement]. */
    fun syncProtocols(statement: StatementNode): Set<Protocol> = statement.syncProtocols
}

/** Returns the union of all sets in this collection. */
private fun <E> Iterable<PersistentSet<E>>.unions(): PersistentSet<E> =
    this.fold(persistentHashSetOf(), PersistentSet<E>::addAll)
