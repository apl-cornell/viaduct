package edu.cornell.cs.apl.viaduct.analysis

import edu.cornell.cs.apl.viaduct.errorskotlin.IllegalInternalCommunicationError
import edu.cornell.cs.apl.viaduct.protocols.Local
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.Variable
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AssertionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BlockNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BreakNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.IfNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InfiniteLoopNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InternalCommunicationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProcessDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReadNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReceiveNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SendNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SimpleStatementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.StatementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.TemporaryDefinition
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.visitors.StatementVisitorWithLocalContext
import edu.cornell.cs.apl.viaduct.syntax.intermediate.visitors.SuspendedTraversal
import edu.cornell.cs.apl.viaduct.syntax.intermediate.visitors.traverse
import edu.cornell.cs.apl.viaduct.util.dataflow.DataFlow
import edu.cornell.cs.apl.viaduct.util.dataflow.DataFlowNode
import edu.cornell.cs.apl.viaduct.util.dataflow.IdentityEdge
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.toPersistentSet
import org.jgrapht.Graph
import org.jgrapht.graph.SimpleDirectedGraph

/**
 * Returns the protocol that coordinates the execution of this statement according to
 * [protocolAssignment].
 *
 * This statement must not be an [InternalCommunicationNode].
 */
fun SimpleStatementNode.primaryProtocol(protocolAssignment: (Variable) -> Protocol): Protocol =
    when (this) {
        is LetNode ->
            protocolAssignment(temporary.value)

        is DeclarationNode ->
            protocolAssignment(variable.value)

        is UpdateNode ->
            protocolAssignment(variable.value)

        is InputNode ->
            protocolAssignment(temporary.value)

        is OutputNode ->
            Local(host.value)

        is InternalCommunicationNode ->
            throw AssertionError()
    }

/**
 * Returns a map from [StatementNode]s in [process] to the sets of [Protocol]s that execute those
 * statements.
 */
fun protocols(
    process: ProcessDeclarationNode,
    protocolAssignment: (Variable) -> Protocol
): Map<StatementNode, Set<Protocol>> =
    ProtocolsCalculator(process, protocolAssignment).compute()

/**
 * Sets up a data flow equation to compute [protocols].
 *
 * Unfortunately, we cannot use a simple recursive traversal to compute [protocols].
 * This is due to [BreakNode]s. Protocols executing a loop must agree on the control flow,
 * which means breaks inside of a loop must be executed by all protocols executing that loop.
 * In terms of math, this means `protocols(break)` equals `protocols(loop_containing_break)`,
 * but `protocols(loop_containing_break)` is the union of protocols of statements inside its body.
 * This includes `break`, so... The circularity means we need data flow.
 */
private class ProtocolsCalculator(
    private val process: ProcessDeclarationNode,
    private val protocolAssignment: (Variable) -> Protocol
) {
    private val readers = readers(process)
    private val definitionSites = definitionSites(process)

    /** An edge `s1 -> s2` indicates that all protocols executing `s1` must also execute `s2`. */
    private val controlDependencyGraph: Graph<Node, IdentityEdge<SetWithUnion<Protocol>>> =
        SimpleDirectedGraph(Edge::class.java)

    // Compute the control graph
    init {
        process.traverse(object : EffectfulExpressionVisitor,
            StatementVisitorWithLocalContext<Unit, Node, Unit, Unit, Node> {
            override fun getData(node: LetNode, value: Unit) = Unit

            override fun getData(node: DeclarationNode, arguments: List<Unit>) = Unit

            override fun getData(node: InputNode) = Unit

            override fun getData(node: ReceiveNode) = Unit

            override fun getData(node: InfiniteLoopNode): Node =
                Node(node)

            override fun leave(node: LetNode, value: Unit): Node =
                Node(node)

            override fun leave(node: DeclarationNode, arguments: List<Unit>): Node =
                Node(node)

            override fun leave(node: UpdateNode, arguments: List<Unit>, data: Unit): Node =
                Node(node)

            override fun leave(node: InputNode): Node =
                Node(node)

            override fun leave(node: OutputNode, message: Unit): Node =
                Node(node)

            override fun leave(node: ReceiveNode): Node =
                Node(node)

            override fun leave(node: SendNode, message: Unit): Node =
                Node(node)

            override fun leave(
                node: IfNode,
                guard: Unit,
                thenBranch: SuspendedTraversal<Node, Unit, Unit, Node, Unit, Unit>,
                elseBranch: SuspendedTraversal<Node, Unit, Unit, Node, Unit, Unit>
            ): Node {
                val graphNode = Node(node)
                controlDependencyGraph.addEdge(thenBranch(this), graphNode)
                controlDependencyGraph.addEdge(elseBranch(this), graphNode)
                node.guard.let {
                    if (it is ReadNode) {
                        val definition = definitionSites.getValue(it.temporary.value)
                        controlDependencyGraph.addEdge(graphNode, Node(definition))
                    }
                }
                return graphNode
            }

            override fun leave(
                node: InfiniteLoopNode,
                body: SuspendedTraversal<Node, Unit, Unit, Node, Unit, Unit>,
                data: Node
            ): Node {
                val graphNode = Node(node)
                controlDependencyGraph.addEdge(body(this), graphNode)
                return graphNode
            }

            override fun leave(node: BreakNode, data: Node): Node {
                val graphNode = Node(node)
                controlDependencyGraph.addEdge(data, graphNode)
                return graphNode
            }

            override fun leave(node: AssertionNode, condition: Unit): Node {
                val graphNode = Node(node)
                // All protocols execute every assertion
                controlDependencyGraph.addEdge(Node(process.body), graphNode)
                return graphNode
            }

            override fun leave(node: BlockNode, statements: List<Node>): Node {
                val graphNode = Node(node)
                statements.forEach {
                    controlDependencyGraph.addEdge(it, graphNode)
                }
                return graphNode
            }
        })
    }

    fun compute(): Map<StatementNode, Set<Protocol>> =
        DataFlow.solve(SetWithUnion.top(), controlDependencyGraph).mapKeys { it.key.statement }

    /** Returns the set of protocols that directly read the temporary defined by this statement. */
    private fun TemporaryDefinition.readingProtocols(): PersistentSet<Protocol> =
        readers.getValue(temporary.value)
            .filterIsInstance<SimpleStatementNode>()
            .map { it.primaryProtocol(protocolAssignment) }
            .toPersistentSet()

    /** A node in the control flow graph. Essentially a statement in the program. */
    private inner class Node(val statement: StatementNode) : DataFlowNode<SetWithUnion<Protocol>> {
        // Note that we pattern match on [statement] and return a function rather than pattern
        // matching _inside_ the function. This way is a lot more efficient since pattern matching
        // is done only once.
        private val transferFunction: (SetWithUnion<Protocol>) -> SetWithUnion<Protocol> =
            when (statement) {
                is LetNode -> {
                    val protocols =
                        statement.readingProtocols()
                            .add(statement.primaryProtocol(protocolAssignment))
                            .run { SetWithUnion(this) };
                    { it.meet(protocols) }
                }

                is DeclarationNode ->
                    always(SetWithUnion(statement.primaryProtocol(protocolAssignment)))

                is UpdateNode ->
                    always(SetWithUnion(statement.primaryProtocol(protocolAssignment)))

                is InputNode -> {
                    val protocols =
                        statement.readingProtocols()
                            .add(statement.primaryProtocol(protocolAssignment))
                            .run { SetWithUnion(this) };
                    { it.meet(protocols) }
                }

                is OutputNode ->
                    always(SetWithUnion(statement.primaryProtocol(protocolAssignment)))

                is InternalCommunicationNode ->
                    throw IllegalInternalCommunicationError(process.protocol.value, statement)

                !is SimpleStatementNode ->
                    { it -> it }
            }

        override fun transfer(`in`: SetWithUnion<Protocol>): SetWithUnion<Protocol> =
            transferFunction(`in`)

        override fun equals(other: Any?): Boolean =
            other is Node && this.statement == other.statement

        override fun hashCode(): Int =
            statement.hashCode()
    }

    /** An edges in the control flow graph. Simply propagates its input without change. */
    private class Edge : IdentityEdge<SetWithUnion<Protocol>>()
}

/** Returns a function that ignores its argument and always returns [result]. */
private fun <T, R> always(result: R): (T) -> R = { _ -> result }
