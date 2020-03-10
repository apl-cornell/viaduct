package edu.cornell.cs.apl.viaduct.analysis

import edu.cornell.cs.apl.attributes.attribute
import edu.cornell.cs.apl.prettyprinting.PrettyPrintable
import edu.cornell.cs.apl.viaduct.errors.ConfidentialityChangingEndorsementError
import edu.cornell.cs.apl.viaduct.errors.InformationFlowError
import edu.cornell.cs.apl.viaduct.errors.InsecureControlFlowError
import edu.cornell.cs.apl.viaduct.errors.InsecureDataFlowError
import edu.cornell.cs.apl.viaduct.errors.IntegrityChangingDeclassificationError
import edu.cornell.cs.apl.viaduct.errors.LabelMismatchError
import edu.cornell.cs.apl.viaduct.errors.MalleableDowngradeError
import edu.cornell.cs.apl.viaduct.security.Label
import edu.cornell.cs.apl.viaduct.security.solver.AtomicLabelTerm
import edu.cornell.cs.apl.viaduct.security.solver.ConstraintSolver
import edu.cornell.cs.apl.viaduct.security.solver.LabelConstant
import edu.cornell.cs.apl.viaduct.security.solver.LabelTerm
import edu.cornell.cs.apl.viaduct.security.solver.LabelVariable
import edu.cornell.cs.apl.viaduct.syntax.HasSourceLocation
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariable
import edu.cornell.cs.apl.viaduct.syntax.Temporary
import edu.cornell.cs.apl.viaduct.syntax.Variable
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AssertionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BlockNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BreakNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclassificationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DowngradeNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.EndorsementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.IfNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InfiniteLoopNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LiteralNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.Node
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OperatorApplicationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.QueryNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReadNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReceiveNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SendNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.StatementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode
import edu.cornell.cs.apl.viaduct.util.FreshNameGenerator
import java.io.Writer

/** Associates [Variable]s with their [Label]s. */
class InformationFlowAnalysis(private val nameAnalysis: NameAnalysis) {
    private val constraintSystem = ConstraintSolver<InformationFlowError>()
    private val nameGenerator = FreshNameGenerator()
    private val solution: Map<LabelVariable, Label> by lazy { constraintSystem.solve() }

    /** The (fresh) variable that stands in for the [Label] of the result of this expression. */
    private val ExpressionNode.labelVariable: LabelVariable by attribute {
        constraintSystem.addNewVariable(PrettyNodeWrapper(this))
    }

    /** The [LabelTerm] representing the [Label] of the temporary defined by this node. */
    private val LetNode.temporaryLabel: LabelVariable by attribute {
        constraintSystem.addNewVariable(PrettyNodeWrapper(this))
    }

    /** The [LabelTerm] representing the [Label] of the object declared by this node. */
    private val DeclarationNode.variableLabel: AtomicLabelTerm by attribute {
        if (labelArguments == null)
            constraintSystem.addNewVariable(PrettyNodeWrapper(variable))
        else {
            // TODO: this is hacky. How do we know it's the first label, for example?
            LabelConstant.create(labelArguments[0].value)
        }
    }

    /** The program counter label at this node. */
    private val Node.pc: PCLabelVariable by attribute {
        val parent = nameAnalysis.tree.parent(this)
        when {
            parent == null ->
                PCLabelVariable("program")
            parent is IfNode && this is BlockNode -> {
                // TODO: two pc variables are generated per IfNode. These variables are equivalent.
                //   Can we cut this down to one?
                val childIndex = nameAnalysis.tree.childIndex(this)
                val newPC = PCLabelVariable("${parent.pc.path}.if.$childIndex")
                parent.pcFlowsTo(this, newPC.variable)
                parent.guard flowsTo newPC.variable
                newPC
            }
            this is InfiniteLoopNode -> {
                val loopPC = PCLabelVariable("${parent.pc.path}.loop")
                parent.pcFlowsTo(jumpLabel, loopPC.variable)
                loopPC
            }
            else ->
                parent.pc
        }
    }

    /**
     * A [LabelVariable] that stands in for the program counter label at some point in the program.
     * The program point is described by [path].
     */
    private inner class PCLabelVariable(val path: String) {
        val variable: LabelVariable =
            constraintSystem.addNewVariable(nameGenerator.getFreshName("$path.pc"))
    }

    /**
     * Adds a constraint asserting that [node] with label [nodeLabel] can flow to a location
     * with label [to].
     */
    private fun assertFlowsTo(node: HasSourceLocation, nodeLabel: AtomicLabelTerm, to: LabelTerm) =
        constraintSystem.addFlowsToConstraint(nodeLabel, to) { actualNodeLabel, toLabel ->
            InsecureDataFlowError(node, actualNodeLabel, toLabel)
        }

    /**
     * Adds a constraint asserting that the program counter label at [this] node can flow into
     * [node], which has label [nodeLabel].
     */
    private fun Node.pcFlowsTo(node: HasSourceLocation, nodeLabel: AtomicLabelTerm) {
        constraintSystem.addFlowsToConstraint(pc.variable, nodeLabel) { pcLabel, actualNodeLabel ->
            InsecureControlFlowError(node, actualNodeLabel, pcLabel)
        }
    }

    /** Asserts that it is safe for [this] node's output to flow to a location with label [to]. */
    private infix fun ExpressionNode.flowsTo(to: LabelTerm) =
        assertFlowsTo(this, labelVariable, to)

    /** Non-recursively add constraints relevant to this expression to [constraintSystem]. */
    private fun ExpressionNode.addConstraints(): Unit =
        when (this) {
            is LiteralNode ->
                Unit
            is ReadNode -> {
                // Note: not leaking the pc since temporaries are "local".
                val temporaryLabel = nameAnalysis.declaration(this).temporaryLabel
                assertFlowsTo(temporary, temporaryLabel, this.labelVariable)
            }
            is OperatorApplicationNode ->
                arguments.forEach { it flowsTo this.labelVariable }
            is QueryNode -> {
                val variableLabel = nameAnalysis.declaration(this).variableLabel

                pcFlowsTo(variable, variableLabel)
                arguments.forEach { it flowsTo variableLabel }

                // TODO: return label should be based on the query.
                //  For example, Array.length leaks the label on the size but not the data.
                assertFlowsTo(variable, variableLabel, this.labelVariable)
            }
            is DowngradeNode -> {
                val from = fromLabel?.let { LabelConstant.create(it.value) } ?: expression.labelVariable
                val to = LabelConstant.create(toLabel.value)

                // The pc is always leaked to the output label
                pcFlowsTo(toLabel, to)

                // From label must match the expression label if it is specified
                if (fromLabel != null) {
                    constraintSystem.addEqualToConstraint(expression.labelVariable, from) { actual, expected ->
                        LabelMismatchError(expression, actual, expected)
                    }
                }

                // Non-malleable downgrade constraints
                constraintSystem.addFlowsToConstraint(from, from.swap().join(to.value)) { _, _ ->
                    MalleableDowngradeError(this)
                }
                constraintSystem.addFlowsToConstraint(from, pc.variable.swap().join(to.value)) { _, _ ->
                    MalleableDowngradeError(this)
                }

                // Check that single dimensional downgrades don't change the other dimension
                when (this) {
                    is DeclassificationNode ->
                        constraintSystem.addEqualToConstraint(from.integrity(), to.integrity()) { fromLabel, _ ->
                            IntegrityChangingDeclassificationError(this, fromLabel)
                        }
                    is EndorsementNode ->
                        constraintSystem.addEqualToConstraint(
                            from.confidentiality(),
                            to.confidentiality()
                        ) { fromLabel, _ ->
                            ConfidentialityChangingEndorsementError(this, fromLabel)
                        }
                }

                assertFlowsTo(toLabel, to, this.labelVariable)
            }
            is InputNode -> {
                val hostLabel = LabelConstant.create(nameAnalysis.declaration(this).authority.value)

                // Host learns the current pc
                pcFlowsTo(host, hostLabel)

                assertFlowsTo(host, hostLabel, this.labelVariable)
            }
            is ReceiveNode -> {
                // TODO: should we leak the pc to the protocol?
                // TODO: should we do any checks on the data?
            }
        }

    /** Non-recursively add constraints relevant to this statement to [constraintSystem]. */
    private fun StatementNode.addConstraints(): Unit =
        when (this) {
            is LetNode -> {
                // Note: not leaking the pc since temporaries are "local".
                value flowsTo temporaryLabel
            }
            is DeclarationNode -> {
                pcFlowsTo(variable, variableLabel)
                arguments.forEach { it flowsTo variableLabel }
            }
            is UpdateNode -> {
                val variableLabel = nameAnalysis.declaration(this).variableLabel
                pcFlowsTo(variable, variableLabel)
                arguments.forEach { it flowsTo variableLabel }
                // TODO: consult the method signature. There may be constraints on the pc or the arguments.
            }

            is OutputNode -> {
                val hostLabel = LabelConstant.create(nameAnalysis.declaration(this).authority.value)
                pcFlowsTo(host, hostLabel)
                message flowsTo hostLabel
            }
            is SendNode -> {
                // TODO: should we leak the pc to the protocol?
                // TODO: should we leak [message] to the protocol?
            }

            is IfNode ->
                Unit
            is InfiniteLoopNode ->
                Unit
            is BreakNode ->
                pcFlowsTo(this, nameAnalysis.loop(this).pc.variable)
            is AssertionNode -> {
                // Everybody must execute assertions, so [condition] must be public and trusted.
                // TODO: can we do any better? This seems almost impossible to achieve...
                condition flowsTo LabelConstant.create(Label.bottom())
            }
            is BlockNode ->
                Unit
        }

    /** Recursively add constraints relevant to this node and all its descendants. */
    private fun Node.addConstraints() {
        when (this) {
            is ExpressionNode ->
                this.addConstraints()
            is StatementNode ->
                this.addConstraints()
        }
        children.forEach { it.addConstraints() }
    }

    init {
        nameAnalysis.tree.root.addConstraints()
    }

    /** Returns the inferred security label of the [Temporary] defined by [node]. */
    fun label(node: LetNode): Label = node.temporaryLabel.getValue(solution)

    /** Returns the inferred security label of the [ObjectVariable] declared by [node]. */
    fun label(node: DeclarationNode): Label = node.variableLabel.getValue(solution)

    /** Returns the inferred security label of the result of [node]. */
    fun label(node: ExpressionNode): Label = node.labelVariable.getValue(solution)

    /**
     * Asserts that the program does not violate information flow security, and throws (a subclass
     * of) [InformationFlowError] otherwise.
     */
    fun check() {
        // Force the thunk, which forces all checks.
        solution
    }

    /** Outputs a DOT representation of the program's constraint graph to [output]. */
    fun exportConstraintGraph(output: Writer) {
        constraintSystem.exportDotGraph(output)
    }
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
