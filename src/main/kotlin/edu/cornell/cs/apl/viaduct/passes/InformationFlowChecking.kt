package edu.cornell.cs.apl.viaduct.passes

import edu.cornell.cs.apl.prettyprinting.PrettyPrintable
import edu.cornell.cs.apl.viaduct.errorskotlin.ConfidentialityChangingEndorsementError
import edu.cornell.cs.apl.viaduct.errorskotlin.InformationFlowError
import edu.cornell.cs.apl.viaduct.errorskotlin.InsecureControlFlowError
import edu.cornell.cs.apl.viaduct.errorskotlin.InsecureDataFlowError
import edu.cornell.cs.apl.viaduct.errorskotlin.IntegrityChangingDeclassificationError
import edu.cornell.cs.apl.viaduct.errorskotlin.LabelMismatchError
import edu.cornell.cs.apl.viaduct.errorskotlin.MalleableDowngradeError
import edu.cornell.cs.apl.viaduct.security.Label
import edu.cornell.cs.apl.viaduct.security.solver.AtomicLabelTerm
import edu.cornell.cs.apl.viaduct.security.solver.ConstraintSolver
import edu.cornell.cs.apl.viaduct.security.solver.LabelConstant
import edu.cornell.cs.apl.viaduct.security.solver.LabelTerm
import edu.cornell.cs.apl.viaduct.security.solver.LabelVariable
import edu.cornell.cs.apl.viaduct.syntax.HasSourceLocation
import edu.cornell.cs.apl.viaduct.syntax.Located
import edu.cornell.cs.apl.viaduct.syntax.Name
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AssertionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BlockNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BreakNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclassificationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DowngradeNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.EndorsementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.HostDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.IfNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InfiniteLoopNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LiteralNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.Node
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OperatorApplicationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProcessDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.QueryNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReadNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReceiveNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SendNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.visitors.ProgramAnnotator
import edu.cornell.cs.apl.viaduct.syntax.intermediate.visitors.StatementVisitorWithContext
import edu.cornell.cs.apl.viaduct.syntax.intermediate.visitors.SuspendedTraversal
import edu.cornell.cs.apl.viaduct.syntax.intermediate.visitors.VariableAnnotationMap
import edu.cornell.cs.apl.viaduct.syntax.intermediate.visitors.annotate
import edu.cornell.cs.apl.viaduct.util.FreshNameGenerator

/**
 * Checks that [this] program does not violate information flow security, and returns a map from
 * variables in each process to their security labels.
 */
fun ProgramNode.checkInformationFlow(): Map<Protocol, VariableAnnotationMap<Label, Label>> {
    return this.annotate(ProgramLabelChecker())
}

// TODO: export constraint graph

private class ProgramLabelChecker :
    ProgramAnnotator<Unit, VariableAnnotationMap<Label, Label>, AtomicLabelTerm, AtomicLabelTerm, LabelConstant, Unit>() {
    override fun getData(node: HostDeclarationNode): LabelConstant =
        LabelConstant.create(node.authority.value)

    override fun getData(node: ProcessDeclarationNode) = Unit

    override fun leaveProcessDeclaration(
        node: ProcessDeclarationNode,
        body: (StatementVisitorWithContext<*, Unit, AtomicLabelTerm, AtomicLabelTerm, *, LabelConstant, Unit>) -> VariableAnnotationMap<AtomicLabelTerm, AtomicLabelTerm>
    ): VariableAnnotationMap<Label, Label> {
        val visitor = StatementLabelChecker()
        val annotations = body(visitor)
        val solution = visitor.constraintSystem.solve()
        return annotations.map({ it.getValue(solution) }, { it.getValue(solution) })
    }
}

private class StatementLabelChecker
private constructor(
    val constraintSystem: ConstraintSolver<InformationFlowError>,
    private val nameGenerator: FreshNameGenerator,

    /** The current program counter label. */
    private val pc: PCLabelVariable
) : StatementVisitorWithContext<AtomicLabelTerm, Unit, AtomicLabelTerm, AtomicLabelTerm, PCLabelVariable, LabelConstant, Unit> {
    companion object {
        /** Base name passed to [nameGenerator] for program counter variables. */
        private const val PC = "pc"

        operator fun invoke(): StatementLabelChecker {
            val constraintSystem = ConstraintSolver<InformationFlowError>()
            val nameGenerator = FreshNameGenerator()
            return StatementLabelChecker(
                constraintSystem,
                nameGenerator,
                PCLabelVariable(constraintSystem.addNewVariable(nameGenerator.getFreshName(PC)))
            )
        }
    }

    /** Returns a copy of this object with some fields changed. */
    private fun copy(pc: PCLabelVariable): StatementLabelChecker =
        StatementLabelChecker(this.constraintSystem, this.nameGenerator, pc)

    /** Returns a newly created label variable that stands in for the [Label] of [name]. */
    private fun freshLabelVariableFor(name: Located<Name>): NodeWithLabel =
        name.withLabel(constraintSystem.addNewVariable(PrettyNodeWrapper(name)))

    /** Returns a newly created label variable that stands in for the [Label] of [node]. */
    private fun freshLabelVariableFor(node: Node): NodeWithLabel =
        node.withLabel(constraintSystem.addNewVariable(PrettyNodeWrapper(node.toSurfaceNode())))

    /** Returns a newly created pc label variable. */
    private fun freshPCLabelVariable(): PCLabelVariable =
        PCLabelVariable(constraintSystem.addNewVariable(nameGenerator.getFreshName(PC)))

    /** Asserts that it is safe for [this] node's output to flow to a location with label [to]. */
    private infix fun NodeWithLabel.flowsTo(to: LabelTerm) {
        constraintSystem.addFlowsToConstraint(this.label, to) { actualNodeLabel, toLabel ->
            InsecureDataFlowError(this.node, actualNodeLabel, toLabel)
        }
    }

    /** Asserts that it is safe for [this] node's output to flow into [node]. */
    private infix fun NodeWithLabel.flowsTo(node: NodeWithLabel) {
        this.flowsTo(node.label)
    }

    /** Asserts that it is safe for [pc] to flow into [node]. */
    private fun pcFlowsTo(node: NodeWithLabel) {
        constraintSystem.addFlowsToConstraint(pc.variable, node.label) { pcLabel, actualNodeLabel ->
            InsecureControlFlowError(node.node, actualNodeLabel, pcLabel)
        }
    }

    /* Context */

    override fun getData(node: LetNode, value: AtomicLabelTerm): AtomicLabelTerm =
        freshLabelVariableFor(node.temporary).also {
            pcFlowsTo(it)
            node.value.withLabel(value) flowsTo it
        }.label

    override fun getData(node: DeclarationNode, arguments: List<AtomicLabelTerm>): AtomicLabelTerm {
        val l =
            if (node.labelArguments == null)
                freshLabelVariableFor(node.variable)
            else
            // TODO: this is hacky. How do we know it's the first label?
                node.variable.withLabel(LabelConstant.create(node.labelArguments[0].value))

        pcFlowsTo(l)
        node.arguments.withLabels(arguments).forEach { it flowsTo l }

        return l.label
    }

    override fun getData(node: InputNode, data: LabelConstant): AtomicLabelTerm =
        freshLabelVariableFor(node.temporary).also {
            pcFlowsTo(it)
            node.host.withLabel(data) flowsTo it
        }.label

    override fun getData(node: ReceiveNode, data: Unit): AtomicLabelTerm =
        freshLabelVariableFor(node.temporary).also {
            pcFlowsTo(it)
            // TODO: should we do any checks on the data?
        }.label

    override fun getData(node: InfiniteLoopNode): PCLabelVariable =
        freshPCLabelVariable().also {
            pcFlowsTo(node.withLabel(it.variable))
        }

    /* Expressions */

    override fun leave(node: LiteralNode): AtomicLabelTerm =
        freshLabelVariableFor(node).label

    override fun leave(node: ReadNode, data: AtomicLabelTerm): AtomicLabelTerm =
        freshLabelVariableFor(node).also {
            // Note: not leaking the pc since temporaries are "local".
            node.temporary.withLabel(data) flowsTo it
        }.label

    override fun leave(
        node: OperatorApplicationNode,
        arguments: List<AtomicLabelTerm>
    ): AtomicLabelTerm =
        freshLabelVariableFor(node).also {
            node.arguments.withLabels(arguments).forEach { argument -> argument flowsTo it }
        }.label

    override fun leave(
        node: QueryNode,
        arguments: List<AtomicLabelTerm>,
        data: AtomicLabelTerm
    ): AtomicLabelTerm {
        val variable = node.variable.withLabel(data)

        pcFlowsTo(variable)
        node.arguments.withLabels(arguments).forEach { it flowsTo variable }

        return freshLabelVariableFor(node).also {
            // TODO: return label should be based on the query.
            //  For example, Array.length leaks the label on the size but not the data.
            variable flowsTo it
        }.label
    }

    private fun leave(node: DowngradeNode, expression: AtomicLabelTerm): AtomicLabelTerm {
        val from = node.fromLabel?.let { LabelConstant.create(it.value) } ?: expression
        val to = LabelConstant.create(node.toLabel.value)

        // The pc is always leaked to the output label
        pcFlowsTo(node.toLabel.withLabel(to))

        // From label must match the expression label if it is specified
        if (node.fromLabel != null) {
            constraintSystem.addEqualToConstraint(expression, from) { actual, expected ->
                LabelMismatchError(node.expression, actual, expected)
            }
        }

        // Non-malleable downgrade constraints
        constraintSystem.addFlowsToConstraint(from, from.swap().join(to.value)) { _, _ ->
            MalleableDowngradeError(node)
        }
        constraintSystem.addFlowsToConstraint(from, pc.variable.swap().join(to.value)) { _, _ ->
            MalleableDowngradeError(node)
        }

        // Check that single dimensional downgrades don't change the other dimension
        when (node) {
            is DeclassificationNode ->
                constraintSystem.addEqualToConstraint(
                    from.integrity(),
                    to.integrity()
                ) { fromLabel, _ ->
                    IntegrityChangingDeclassificationError(node, fromLabel)
                }
            is EndorsementNode ->
                constraintSystem.addEqualToConstraint(
                    from.confidentiality(),
                    to.confidentiality()
                ) { fromLabel, _ ->
                    ConfidentialityChangingEndorsementError(node, fromLabel)
                }
        }

        return freshLabelVariableFor(node).also {
            node.toLabel.withLabel(to) flowsTo it
        }.label
    }

    override fun leave(node: DeclassificationNode, expression: AtomicLabelTerm): AtomicLabelTerm =
        leave(node as DowngradeNode, expression)

    override fun leave(node: EndorsementNode, expression: AtomicLabelTerm): AtomicLabelTerm =
        leave(node as DowngradeNode, expression)

    /* Statements */

    override fun leave(node: LetNode, value: AtomicLabelTerm) = Unit

    override fun leave(node: DeclarationNode, arguments: List<AtomicLabelTerm>) = Unit

    override fun leave(node: UpdateNode, arguments: List<AtomicLabelTerm>, data: AtomicLabelTerm) {
        val variable = node.variable.withLabel(data)
        pcFlowsTo(variable)
        node.arguments.withLabels(arguments).forEach { it flowsTo variable }
        // TODO: consult the method signature. There may be constraints on the pc or the arguments.
    }

    override fun leave(node: InputNode, data: LabelConstant) {
        pcFlowsTo(node.host.withLabel(data))
    }

    override fun leave(node: OutputNode, message: AtomicLabelTerm, data: LabelConstant) {
        val host = node.host.withLabel(data)
        pcFlowsTo(host)
        node.message.withLabel(message).flowsTo(host)
    }

    override fun leave(node: ReceiveNode, data: Unit) {
        // TODO: should we leak the pc to the protocol?
    }

    override fun leave(node: SendNode, message: AtomicLabelTerm, data: Unit) {
        // TODO: should we leak the pc to the protocol?
        // TODO: should we leak message to the protocol?
    }

    override fun leave(
        node: IfNode,
        guard: AtomicLabelTerm,
        thenBranch: SuspendedTraversal<Unit, AtomicLabelTerm, AtomicLabelTerm, PCLabelVariable, LabelConstant, Unit>,
        elseBranch: SuspendedTraversal<Unit, AtomicLabelTerm, AtomicLabelTerm, PCLabelVariable, LabelConstant, Unit>
    ) {
        val newPC = freshPCLabelVariable()
        pcFlowsTo(node.withLabel(newPC.variable))
        node.guard.withLabel(guard) flowsTo newPC.variable

        // Check branches under the updated pc.
        val visitor = this.copy(pc = newPC)
        thenBranch(visitor)
        elseBranch(visitor)
    }

    override fun leave(
        node: InfiniteLoopNode,
        body: SuspendedTraversal<Unit, AtomicLabelTerm, AtomicLabelTerm, PCLabelVariable, LabelConstant, Unit>,
        data: PCLabelVariable
    ) {
        body(this.copy(pc = data))
    }

    override fun leave(node: BreakNode, data: PCLabelVariable) {
        pcFlowsTo(node.withLabel(data.variable))
    }

    override fun leave(node: AssertionNode, condition: AtomicLabelTerm) {
        // Everybody must execute assertions, so [condition] must be public and trusted.
        // TODO: can we do any better? This seems almost impossible to achieve...
        node.condition.withLabel(condition) flowsTo LabelConstant.create(Label.bottom())
    }

    override fun leave(node: BlockNode, statements: List<Unit>) = Unit
}

/**
 * A [LabelVariable] that stands in for the program counter label at some point in the program.
 *
 * This is nothing more than a wrapper around [LabelVariable]. We use it for clarity.
 */
private data class PCLabelVariable(val variable: LabelVariable)

/** Pairs [node] with its [label]. */
private data class NodeWithLabel(val node: HasSourceLocation, val label: AtomicLabelTerm)

/** Pairs [this] node with its [label]. */
private fun HasSourceLocation.withLabel(label: AtomicLabelTerm) = NodeWithLabel(this, label)

/** Pairs all nodes in [this] with their [labels]. */
private fun List<HasSourceLocation>.withLabels(labels: List<AtomicLabelTerm>): List<NodeWithLabel> {
    assert(this.size == labels.size)
    return this.zip(labels, HasSourceLocation::withLabel)
}

/**
 * A wrapper around AST nodes whose only job is to pretty print the node when [toString] is called.
 * Instances of this class are used as [LabelVariable] labels in the [ConstraintSolver] so we get
 * more readable debug output.
 */
private class PrettyNodeWrapper(private val node: PrettyPrintable) {
    // TODO: colors?
    override fun toString(): String = node.asDocument.print()
}
